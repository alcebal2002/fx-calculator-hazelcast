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

public class RunnableThreadSpread implements Runnable {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(RunnableThreadSpread.class);

	private String currentCurrency;
	private Properties applicationProperties;
	private ExecutionTask executionTask;

	private Map<String, List<FxRate>> historicalDataMap = new HashMap<String, List<FxRate>>();
	private Map<String, Integer> resultsMap = new HashMap<String, Integer>();
	
	private long elapsedTimeMillis;
	private long totalHistDataLoaded = 0;
	private long totalCalculations = 0;

	public RunnableThreadSpread (ExecutionTask executionTask){
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
			float spread = 0;
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency);
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency);

				calculationStartTime = System.currentTimeMillis();
				
				logger.info ("Retrieving spread data for " + currentCurrency);
				spread = GeneralUtils.getSpread(currentCurrency,applicationProperties);
				logger.info ("Starting spread calculations for " + currentCurrency);
				totalCalculations += executeSpreadCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels, spread);
				
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

	// Executes calculations with Spreads (levels)
    public long executeSpreadCalculation (final String currentCurrency, final float increasePercentage, final float decreasePercentage, final int maxLevels, final float spread) {
    	
    	long totalCalculations = 0;
		float increase = (1+(increasePercentage)/100);
    	float decrease = (1-(decreasePercentage)/100);
		
    	StringBuilder result;
    	long found;
    	
		if (historicalDataMap.containsKey(currentCurrency)) {

			for (FxRate originalFxRate : historicalDataMap.get(currentCurrency)) {
				
				int positionId = originalFxRate.getPositionId();
				float opening = originalFxRate.getOpen();
				
				logger.debug ("Processing " + currentCurrency + "-" + positionId);
				
				FxRate targetFxRate = null;
		    	result =  new StringBuilder();
		    	found = 0;
				
				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);
					
					logger.debug ("Comparing against " + targetFxRate.getCurrencyPair() + "-" + targetFxRate.getPositionId());

					if ((targetFxRate.getHigh() > (opening * increase)-spread)) {
						result.append("S");
						opening = (opening * increase)-spread;
						found++;
					} else if ((targetFxRate.getLow() < (opening * decrease)+spread)) {
						result.append("B");
						opening = (opening * decrease)+spread;
						found++;
					}
					
					totalCalculations++;

					if (found == maxLevels) {
						GeneralUtils.increaseMapCounter (resultsMap, result.toString());
						break;
					}

				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid Spread calculation");
		}
		logger.info("Spread result: " + resultsMap.toString());
		return totalCalculations;
    }

	public Map<String, Integer> getResultsMap () { return resultsMap; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalHistDataLoaded () {	return this.totalHistDataLoaded; }
	public long getElapsedTimeMillis () { return this.elapsedTimeMillis; }
}
