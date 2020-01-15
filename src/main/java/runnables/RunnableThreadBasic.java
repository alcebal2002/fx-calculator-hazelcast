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
import utils.Constants;
import utils.GeneralUtils;

public class RunnableThreadBasic implements RunnableCalculation, Runnable {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(RunnableThreadBasic.class);

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

	public RunnableThreadBasic (ExecutionTask executionTask) {
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
			float increasePercentage = Float.parseFloat(applicationProperties.getProperty(Constants.AP_INCREASEPERCENTAGE));
			float decreasePercentage = Float.parseFloat(applicationProperties.getProperty(Constants.AP_DECREASEPERCENTAGE));
			int maxLevels = Integer.parseInt(applicationProperties.getProperty(Constants.AP_MAXLEVELS));
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency + " - " + executionTask.getTaskType());
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,startDate,endDate,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency + " - " + executionTask.getTaskType());

				logger.info ("Starting calculations for " + currentCurrency + " - " + executionTask.getTaskType());
				totalCalculations += executeCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels);
				
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
    
	// Executes Basic calculations (levels)
    public long executeCalculation (final String currentCurrency, final float increasePercentage, final float decreasePercentage, final int maxLevels) {
    	
    	long totalCalculations = 0;
		float increase = (1+(increasePercentage)/100);
    	float decrease = (1-(decreasePercentage)/100);
    	
		if (historicalDataMap.containsKey(currentCurrency)) {

			for (FxRate originalFxRate : historicalDataMap.get(currentCurrency)) {
				
				int positionId = originalFxRate.getPositionId();
				float opening = originalFxRate.getOpen();
				
				if (logger.isDebugEnabled())
					logger.debug ("Processing " + currentCurrency + "-" + positionId);
				
				FxRate targetFxRate = null;
				String previousFound = "";
				
				int indexUp = 1;
				int indexDown = 1;

				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);

					totalCalculations++;
					
					if (logger.isDebugEnabled())
						logger.debug ("Comparing against " + targetFxRate.getCurrencyPair() + "-" + targetFxRate.getPositionId());
					
					if ((targetFxRate.getHigh() > (opening * increase)) && (indexUp <= maxLevels)) {
						if (("DOWN").equals(previousFound)) {
							break;
						}

						GeneralUtils.increaseMapCounter (resultsMap, ("UP-"+indexUp));

						previousFound = "UP";
						opening = opening * increase;
						indexUp++;
					} else if ((targetFxRate.getLow() < (opening * decrease)) && (indexDown <= maxLevels)) {
						if (("UP").equals(previousFound)) {
							break;
						}

						GeneralUtils.increaseMapCounter (resultsMap, ("DOWN-"+indexDown));

						previousFound = "DOWN";
						opening = opening * decrease;
						indexDown++;
					}

					// No need to continue if maxLevels have been exceeded
					if (indexUp > maxLevels && indexDown > maxLevels) {
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
