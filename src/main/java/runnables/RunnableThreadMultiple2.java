package runnables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datamodel.CalculationResult;
import datamodel.ExecutionTask;
import datamodel.FxRate;
import utils.ApplicationProperties;
import utils.Constants;
import utils.GeneralUtils;

public class RunnableThreadMultiple2 implements RunnableCalculation, Runnable {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(RunnableThreadSpread.class);

	private String currentCurrency;
	private String startDate;
	private String endDate;
	private Properties applicationProperties;
	private ExecutionTask executionTask;

	private Map<String, List<FxRate>> historicalDataMap = new HashMap<String, List<FxRate>>();
	private Map<String, Integer> resultsMap = new HashMap<String, Integer>();
	
	private long startTime;
	private long stopTime;
	private long elapsedTime;
	private long totalHistDataLoaded = 0;
	private long totalCalculations = 0;

	public RunnableThreadMultiple2 (ExecutionTask executionTask){
		this.executionTask = executionTask;
		this.applicationProperties = executionTask.getApplicationParameters();
		this.currentCurrency = executionTask.getCurrentCurrency();
		this.startDate = executionTask.getStartDate();
		this.endDate = executionTask.getEndDate();
	}
	
	@Override
	public void run() {
		
		startTime = System.currentTimeMillis();
		
		try {
			
			// Calculates required properties based on the application properties retrieved from the execution task
			List<String> multipleCalculations = ApplicationProperties.getListProperty(Constants.AP_MULTIPLE);
			
			float spread = 0;
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency + " - " + executionTask.getTaskType());
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,startDate,endDate,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency + " - " + executionTask.getTaskType());

				logger.info ("Retrieving spread data for " + currentCurrency + " - " + executionTask.getTaskType());
				spread = GeneralUtils.getSpread(currentCurrency,applicationProperties);
				logger.info ("Starting calculations for " + currentCurrency + " - " + executionTask.getTaskType());
				totalCalculations += executeCalculation (currentCurrency, multipleCalculations, multipleCalculations.size(), spread);
				
				stopTime = System.currentTimeMillis();
				elapsedTime = stopTime - startTime;

				logger.info ("Finished calculations for " + currentCurrency + " - " + executionTask.getTaskType() + " [#calcs: " + totalCalculations + " - #results: " + resultsMap.size() + "] in " + elapsedTime + " ms");
			} else {
				logger.error("No available data for " + currentCurrency + " - " + executionTask.getTaskType());
			}

			logger.info ("Populating Calculation Result Map for " + currentCurrency + " - " + executionTask.getTaskType());
			// Populates the Calculation Result Map
			executionTask.setCalculationResult(new CalculationResult(startTime, stopTime, totalHistDataLoaded, totalCalculations, resultsMap));
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}

	// Executes 1212 calculations (levels)
    public long executeCalculation (final String currentCurrency, final List<String> multipleCalculations, final int maxLevels, final float spread) {

    	int multiplePosition = 0;
    	
    	float firstIncrease = (1+(getMultiplePercentage(multipleCalculations, multiplePosition))/100);
    	float firstDecrease = (1-(getMultiplePercentage(multipleCalculations, multiplePosition))/100);
    	String firstDirection = getMultipleDirection (multipleCalculations, multiplePosition);
    	
    	float selectedIncrease = firstIncrease;
    	float selectedDecrease = firstDecrease;
    	String selectedDirection = firstDirection;
    	
    	long totalCalculations = 0;
    	long changeCounter = 1;
    	FxRate targetFxRate = null;
    	
		if (historicalDataMap.containsKey(currentCurrency)) {

			//System.out.print ("Original Row|Original Date|Original Time|Target Row|Target Date|Target Time|Selected Increase|Selected Decrease|Second Iteration ?|Previous Found|Opening|Condition|Iterations/#Max Iterations|Change|Break");
			
			for (FxRate originalFxRate : historicalDataMap.get(currentCurrency)) {
				
				int positionId = originalFxRate.getPositionId();
				float opening = originalFxRate.getOpen();
				
				if (logger.isDebugEnabled())
					logger.debug ("Processing " + currentCurrency + "-" + positionId);
				
				changeCounter = 1;
				multiplePosition = 0;
				selectedIncrease = firstIncrease;
				selectedDecrease = firstDecrease;
				selectedDirection = firstDirection;
				
				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);

					selectedIncrease = (1+(getMultiplePercentage(multipleCalculations, multiplePosition))/100);
					selectedDecrease = (1-(getMultiplePercentage(multipleCalculations, multiplePosition))/100);
					selectedDirection = getMultipleDirection (multipleCalculations, multiplePosition);

					totalCalculations++;
					
					if (targetFxRate.getHigh() > (opening * selectedIncrease) - spread) {
						
						if (("DOWN").equals(selectedDirection)) {
							if (logger.isDebugEnabled())
								logger.debug("-BREAK ("+selectedDecrease+")");
							break;
						}

						GeneralUtils.increaseMapCounter (resultsMap, ("UP-"+changeCounter));

						opening = (opening * selectedIncrease) - spread;
						changeCounter++;
						multiplePosition++;
					} else if (targetFxRate.getLow() < (opening * selectedDecrease) + spread) {
						
						if (("UP").equals(selectedDirection)) {
							if (logger.isDebugEnabled())
								logger.debug("-BREAK ("+selectedDecrease+")");
							break;
						}

						GeneralUtils.increaseMapCounter (resultsMap, ("DOWN-"+changeCounter));

						opening = (opening * selectedDecrease) + spread;
						changeCounter++;
						multiplePosition++;
					}
					
					// No need to continue if maxLevels have been exceeded
					if (changeCounter == maxLevels) {
						break;
					}
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + " - " + executionTask.getTaskType() + ". Avoiding calculation");
		}			
		return totalCalculations;
    }
    
    public String getMultipleDirection (final List<String> multipleCalculations, final int multiplePosition) {
    	return multipleCalculations.get(multiplePosition).substring(0, multipleCalculations.get(multiplePosition).indexOf("-"));
    }

    public float getMultiplePercentage (final List<String> multipleCalculations, final int multiplePosition) {
    	return Float.parseFloat(multipleCalculations.get(multiplePosition).substring(multipleCalculations.get(multiplePosition).indexOf("-")+1));
    }

	public ExecutionTask getExecutionTask() { return this.executionTask; }
}
