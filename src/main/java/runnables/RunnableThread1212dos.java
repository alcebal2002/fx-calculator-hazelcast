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
import utils.GeneralUtils;

public class RunnableThread1212dos implements RunnableCalculation, Runnable {

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

	public RunnableThread1212dos (ExecutionTask executionTask){
		this.executionTask = executionTask;
		this.applicationProperties = executionTask.getApplicationParameters();
		this.currentCurrency = executionTask.getCurrentCurrency();
		this.startDate = executionTask.getStartDate();
		this.endDate = executionTask.getEndDate();	}
	
	@Override
	public void run() {
		
		startTime = System.currentTimeMillis();
		
		try {
			
			// Calculates required properties based on the application properties retrieved from the execution task
			float increasePercentage = Float.parseFloat(applicationProperties.getProperty("application.increasePercentage"));
			float decreasePercentage = Float.parseFloat(applicationProperties.getProperty("application.decreasePercentage"));
			int maxLevels = Integer.parseInt(applicationProperties.getProperty("application.maxLevels"));
//			int maxFirstIterations = Integer.parseInt(applicationProperties.getProperty("application.maxFirstIterations"));
			
			float spread = 0;
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency + " - " + executionTask.getTaskType());
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,startDate,endDate,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency + " - " + executionTask.getTaskType());

				logger.info ("Retrieving spread data for " + currentCurrency + " - " + executionTask.getTaskType());
				spread = GeneralUtils.getSpread(currentCurrency,applicationProperties);
				logger.info ("Starting calculations for " + currentCurrency + " - " + executionTask.getTaskType());
				totalCalculations += executeCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels, spread);
				
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

	// Executes 1212dos calculations (levels)
    public long executeCalculation (final String currentCurrency, final float firstPercentage, final float secondPercentage, final int maxLevels, final float spread) {
    	
    	float firstIncrease = (1+(firstPercentage)/100);
    	float firstDecrease = (1-(firstPercentage)/100);
    	float secondIncrease = (1+(secondPercentage)/100);
    	float secondDecrease = (1-(secondPercentage)/100);
    	float selectedIncrease = firstIncrease;
    	float selectedDecrease = firstDecrease;
    	
    	long totalCalculations = 0;
    	long changeCounter = 1;
    	FxRate targetFxRate = null;
    	String previousFound = null;
    	
		if (historicalDataMap.containsKey(currentCurrency)) {

			//System.out.print ("Original Row|Original Date|Original Time|Target Row|Target Date|Target Time|Selected Increase|Selected Decrease|Second Iteration ?|Previous Found|Opening|Condition|Iterations/#Max Iterations|Change|Break");
			
			for (FxRate originalFxRate : historicalDataMap.get(currentCurrency)) {
				
				int positionId = originalFxRate.getPositionId();
				float opening = originalFxRate.getOpen();
				
				if (logger.isDebugEnabled())
					logger.debug ("Processing " + currentCurrency + "-" + positionId);
				
				previousFound = null;
				
				changeCounter = 1;
				selectedIncrease = firstIncrease;
				selectedDecrease = firstDecrease;
				
				//System.out.println ("Processing PositionId: " + positionId + " with percentages -> " + selectedIncrease + " - " + selectedDecrease);

				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);

					totalCalculations++;
										
					//System.out.print ("Comparing " + positionId + " vs " + targetFxRate.getPositionId() + " [" + selectedIncrease + " - " + selectedDecrease + "] [secondIteration " + secondIteration + "] [previousFound " + previousFound+ "]");
					
					if (targetFxRate.getHigh() > (opening * selectedIncrease) - spread) {
						
						//System.out.print (positionId + "|" + originalFxRate.getConversionDate() + "|" + originalFxRate.getConversionTime() + "|" + targetFxRate.getPositionId() + "|" + targetFxRate.getConversionDate() + "|" + targetFxRate.getConversionTime() + "|" + selectedIncrease + "|" + selectedDecrease + "|" + secondIteration + "|" + previousFound+ "|" + opening + "|" + targetFxRate.getHigh() +" > " + ((opening * selectedIncrease) - spread) + "|" + changeCounter + " / " + maxFirstIterations + "|UP");

						if (("UP").equals(previousFound)) {
							//System.out.println ("|BREAK");
							break;
						}
						GeneralUtils.increaseMapCounter (resultsMap, ("UP-"+changeCounter));
						
						changeCounter++;
						previousFound = "UP";
						opening = (opening * selectedIncrease) - spread;
						
						if (selectedIncrease == firstIncrease) {
							selectedIncrease = secondIncrease;
							selectedDecrease = secondDecrease;
						} else {
							selectedIncrease = firstIncrease;
							selectedDecrease = firstDecrease;
						}
						
					} else if (targetFxRate.getLow() < (opening * selectedDecrease) + spread) {
						
						//System.out.print (positionId + "|" + originalFxRate.getConversionDate() + "|" + originalFxRate.getConversionTime() + "|" + targetFxRate.getPositionId() + "|" + targetFxRate.getConversionDate() + "|" + targetFxRate.getConversionTime() + "|" + selectedIncrease + "|" + selectedDecrease + "|" + secondIteration + "|" + previousFound+ "|" + opening + "|" + targetFxRate.getHigh() +" < " + ((opening * selectedIncrease) + spread) + "|" + changeCounter + " / " + maxFirstIterations + "|DOWN");

						if (("DOWN").equals(previousFound)) {
							//System.out.println ("|BREAK");
							if (logger.isDebugEnabled())
								logger.debug("-BREAK ("+selectedDecrease+")");
							break;
						}
						GeneralUtils.increaseMapCounter (resultsMap, ("DOWN-"+changeCounter));

						changeCounter++;
						previousFound = "DOWN";
						opening = (opening * selectedDecrease) + spread;

						if (selectedIncrease == firstIncrease) {
							selectedIncrease = secondIncrease;
							selectedDecrease = secondDecrease;
						} else {
							selectedIncrease = firstIncrease;
							selectedDecrease = firstDecrease;
						}

					}
					
					// No need to continue if maxLevels have been exceeded
					if (changeCounter == maxLevels) {
						//System.out.println ("|BREAK - MaxLevels detected (" + changeCounter+")");
						break;
					}
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + " - " + executionTask.getTaskType() + ". Avoiding calculation");
		}
				
		return totalCalculations;
    }
    
	public ExecutionTask getExecutionTask() { return this.executionTask; }
}
