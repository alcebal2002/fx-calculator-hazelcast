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

public class RunnableThread1234 implements Runnable {

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

	public RunnableThread1234 (ExecutionTask executionTask){
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
			int maxFirstIterations = Integer.parseInt(applicationProperties.getProperty("application.maxFirstIterations"));
			
			float spread = 0;
			
			if (GeneralUtils.checkIfCurrencyExists (currentCurrency,applicationProperties)) {

				logger.info ("Populating historical data for " + currentCurrency);
				totalHistDataLoaded = GeneralUtils.populateHistoricalFxData(currentCurrency,historicalDataMap,applicationProperties);
				logger.info ("Historical data populated for " + currentCurrency);

				calculationStartTime = System.currentTimeMillis();
				
				logger.info ("Retrieving spread data for " + currentCurrency);
				spread = GeneralUtils.getSpread(currentCurrency,applicationProperties);
				logger.info ("Starting " + executionTask.getCalculationMethodology() + " calculations for " + currentCurrency);
				totalCalculations += execute1234Calculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels, spread, maxFirstIterations);
				
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

		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}

	// Executes 1234 calculations (levels)
    public long execute1234Calculation (final String currentCurrency, final float firstPercentage, final float secondPercentage, final int maxLevels, final float spread, final int maxFirstIterations) {
    	
    	long totalCalculations = 0;
    	float firstIncrease = (1+(firstPercentage)/100);
    	float firstDecrease = (1-(firstPercentage)/100);
    	float secondIncrease = (1+(secondPercentage)/100);
    	float secondDecrease = (1-(secondPercentage)/100);
    	float selectedIncrease = firstIncrease;
    	float selectedDecrease = firstDecrease;
    	
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

					// Avoid assigning all the time. Just once
					if ((indexUp > maxFirstIterations) && (indexDown > maxFirstIterations)) {
						selectedIncrease = secondIncrease;
				    	selectedDecrease = secondDecrease;
					}
					
					if ((targetFxRate.getHigh() > (opening * selectedIncrease) - spread)  && (indexUp <= maxLevels)) {
						if (("DOWN").equals(previousFound)) {
							break;
						}
						GeneralUtils.increaseMapCounter (resultsMap, ("UP-"+indexUp));
						
						indexUp++;
						previousFound = "UP";
						opening = (opening * selectedIncrease) - spread;
					} else if ((targetFxRate.getLow() < (opening * selectedDecrease) + spread) && (indexDown <= maxLevels)) {
						if (("UP").equals(previousFound)) {
							break;
						}
						GeneralUtils.increaseMapCounter (resultsMap, ("DOWN-"+indexDown));

						indexDown++;
						previousFound = "DOWN";
						opening = (opening * selectedDecrease) + spread;
					}
					totalCalculations++;

					// No need to continue if maxLevels have been exceeded
					if (indexUp > maxLevels && indexDown > maxLevels) {
						break;
					}
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid 1234 calculation");
		}
		return totalCalculations;
    }
    
	public Map<String, Integer> getResultsMap () { return resultsMap; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalHistDataLoaded () {	return this.totalHistDataLoaded; }
	public long getElapsedTimeMillis () { return this.elapsedTimeMillis; }
	public ExecutionTask getExecutionTask() { return this.executionTask; }
}
