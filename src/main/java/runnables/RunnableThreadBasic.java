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

public class RunnableThreadBasic implements Runnable {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(RunnableThreadBasic.class);

	private String currentCurrency;
	private Properties applicationProperties;
	private ExecutionTask executionTask;

	private Map<String, List<FxRate>> historicalDataMap = new HashMap<String, List<FxRate>>();
	private Map<String, Integer> resultsMap = new HashMap<String, Integer>();
	
	private long elapsedTimeMillis;
	private long totalHistDataLoaded = 0;
	private long totalCalculations = 0;

	public RunnableThreadBasic (ExecutionTask executionTask) {
		this.executionTask = executionTask;
		this.applicationProperties = executionTask.getTaskParameters();
		this.currentCurrency = executionTask.getCurrentCurrency();
	}
	
	@Override
	public void run() {
		
		long calculationStartTime;
		long calculationStopTime;

		long startTime = System.currentTimeMillis();
		
		try {
			
			// Calculates required properties based on the application properties retrieved from the execution task
			float increasePercentage = Float.parseFloat(applicationProperties.getProperty("application.increasePercentage"));
			float decreasePercentage = Float.parseFloat(applicationProperties.getProperty("application.decreasePercentage"));
			int maxLevels = Integer.parseInt(applicationProperties.getProperty("application.maxLevels"));
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency);
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency);

				calculationStartTime = System.currentTimeMillis();
				logger.info ("Starting basic calculations for " + currentCurrency);
				totalCalculations += executeBasicCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels);
				
				calculationStopTime = System.currentTimeMillis();

				logger.info ("Finished calculations for " + currentCurrency + " [" + totalCalculations + "] in " + (calculationStopTime - calculationStartTime) + " ms");
				
			} else {
				logger.error("No available data for " + currentCurrency);
			}

			long stopTime = System.currentTimeMillis(); 
			elapsedTimeMillis = stopTime - startTime;

			logger.debug ("Populating Calculation Result Map for " + currentCurrency + " - " + executionTask.getCalculationMethodology());
			// Populates the Calculation Result Map
			executionTask.setCalculationResult(new CalculationResult(startTime, stopTime, totalHistDataLoaded, totalCalculations, resultsMap));

			logger.debug ("Putting Results for " + currentCurrency + " - " + executionTask.getCalculationMethodology() + " into Hazelcast");
			
			// PUT INTO HAZELCAST

			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
    
	// Executes Basic calculations (levels)
    public long executeBasicCalculation (final String currentCurrency, final float increasePercentage, final float decreasePercentage, final int maxLevels) {
    	
    	long totalCalculations = 0;
		float increase = (1+(increasePercentage)/100);
    	float decrease = (1-(decreasePercentage)/100);
    	
		if (historicalDataMap.containsKey(currentCurrency)) {

			for (FxRate originalFxRate : historicalDataMap.get(currentCurrency)) {
				
				int positionId = originalFxRate.getPositionId();
				float opening = originalFxRate.getOpen();
				
				logger.debug ("Processing " + currentCurrency + "-" + positionId);
				
				FxRate targetFxRate = null;
				String previousFound = "";
				
				int indexUp = 1;
				int indexDown = 1;

				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);

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
					
					totalCalculations++;

					// No need to continue if maxLevels have been exceeded
					if (indexUp > maxLevels && indexDown > maxLevels) {
						break;
					}
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid Basic calculation");
		}
		return totalCalculations;
    }

	public Map<String, Integer> getResultsMap () { return resultsMap; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalHistDataLoaded () {	return this.totalHistDataLoaded; }
	public long getElapsedTimeMillis () { return this.elapsedTimeMillis; }
}
