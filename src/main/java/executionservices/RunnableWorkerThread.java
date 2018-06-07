package executionservices;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;

import datamodel.CalcResult;
import datamodel.ExecutionTask;
import datamodel.FxRate;
import utils.ApplicationProperties;
import utils.DatabaseUtils;
import utils.GeneralUtils;

public class RunnableWorkerThread implements Runnable {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(RunnableWorkerThread.class);

	private String currentCurrency;
	private Properties applicationProperties;

	private Map<String, List<FxRate>> historicalDataMap = new HashMap<String, List<FxRate>>();
	private Map<String, Integer> basicResultsMap = new HashMap<String, Integer>();
	private Map<String, Integer> spreadResultsMap = new HashMap<String, Integer>();
	private float spread = 0;
	private Map<String, CalcResult> calcResultsMap;
	
	private long elapsedTimeMillis;
	private long totalHistDataLoaded = 0;
	private long totalCalculations = 0;
	private long totalBasicResults = 0;
	private long totalSpreadResults = 0;


	public RunnableWorkerThread (final ExecutionTask executionTask, Map<String, CalcResult> calcResultsMap){
		this.applicationProperties = executionTask.getTaskParameters();
		this.currentCurrency = executionTask.getCurrentCurrency();
		this.calcResultsMap = calcResultsMap;
	}
	
	@Override
	public void run() {
		
		long histDataStartTime;	
		long histDataStopTime;
		long calculationStartTime;
		long calculationStopTime;

		long startTime = System.currentTimeMillis();
		
		try {
			
			// Load required properties
			float increase = (1+(Float.parseFloat(applicationProperties.getProperty("application.increasePercentage")))/100);
			float decrease = (1-(Float.parseFloat(applicationProperties.getProperty("application.decreasePercentage")))/100);
			int maxLevels = Integer.parseInt(applicationProperties.getProperty("application.maxLevels"));
			String startDate = applicationProperties.getProperty("application.startDate");
			String endDate = applicationProperties.getProperty("application.endDate");

			if (checkIfCurrencyExists (currentCurrency)) {

				logger.info ("Populating historical data for " + currentCurrency);
				histDataStartTime = System.currentTimeMillis();
				totalHistDataLoaded = populateHistoricalFxData(currentCurrency,startDate,endDate);
				histDataStopTime = System.currentTimeMillis();
				logger.info ("Historical data populated for " + currentCurrency);

				calculationStartTime = System.currentTimeMillis();
				
				if ((applicationProperties.getProperty("application.calculations")).toLowerCase().contains("basic")) {
					logger.info ("Starting basic calculations for " + currentCurrency);
					totalCalculations += executeBasicCalculation (currentCurrency, increase, decrease, maxLevels);
				}
				if ((applicationProperties.getProperty("application.calculations")).toLowerCase().contains("spread")) {
					logger.info ("Retrieving spread data for " + currentCurrency);
					spread = getSpread(currentCurrency);
					logger.info ("Starting spread calculations for " + currentCurrency);
					totalCalculations += executeSpreadCalculation (currentCurrency, increase, decrease, maxLevels, spread);
				}
				
				calculationStopTime = System.currentTimeMillis();

				totalBasicResults = basicResultsMap.size();
				totalSpreadResults = spreadResultsMap.size();

				logger.debug ("Populating Calculation Result Map for " + currentCurrency);
				// Populates the Calculation Result Map
				calcResultsMap.put(currentCurrency, new CalcResult(currentCurrency, increase, decrease, maxLevels, spread, histDataStartTime, histDataStopTime, totalHistDataLoaded, calculationStartTime, calculationStopTime, totalCalculations, basicResultsMap, spreadResultsMap));

				logger.info ("Finished calculations for " + currentCurrency + " [" + totalCalculations + "] in " + (calculationStopTime - calculationStartTime) + " ms");
			} else {
				logger.error("No available data for " + currentCurrency);
			}

			long stopTime = System.currentTimeMillis(); 
			elapsedTimeMillis = stopTime - startTime;
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}

	public boolean checkIfCurrencyExists (final String currentCurrency) {

		boolean exists = false;

		if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
			exists = DatabaseUtils.checkCurrencyTableExists(currentCurrency);
		} else {
			exists = GeneralUtils.checkIfFileExists(currentCurrency);
		}
		return exists;
	}

	// Executes calculations with Spreads (levels)
    public long executeSpreadCalculation (final String currentCurrency, final float increase, final float decrease, final int maxLevels, final float spread) {
    	
    	long totalCalculations = 0;
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
						if (spreadResultsMap.containsKey(result.toString())) {
							spreadResultsMap.put(result.toString(),spreadResultsMap.get(result.toString())+1);
						} else {
							spreadResultsMap.put(result.toString(),1);
						}
						found = 0;
						break;
					}

				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid Spread calculation");
		}
		logger.info("Spread result: " + spreadResultsMap.toString());
		return totalCalculations;
    }
    
	// Executes calculations (levels)
    public long executeBasicCalculation (final String currentCurrency, float increase, float decrease, int maxLevels) {
    	
    	long totalCalculations = 0;
    	
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

						if (basicResultsMap.containsKey("UP-"+indexUp)) {
							basicResultsMap.put("UP-"+indexUp,basicResultsMap.get("UP-"+indexUp)+1);
						} else {
							basicResultsMap.put("UP-"+indexUp,1);
						}

						previousFound = "UP";
						opening = opening * increase;
						indexUp++;
					} else if ((targetFxRate.getLow() < (opening * decrease)) && (indexDown <= maxLevels)) {
						if (("UP").equals(previousFound)) {
							break;
						}

						if (basicResultsMap.containsKey("DOWN-"+indexDown)) {
							basicResultsMap.put("DOWN-"+indexDown,basicResultsMap.get("DOWN-"+indexDown)+1);
						} else {
							basicResultsMap.put("DOWN-"+indexDown,1);
						}

						previousFound = "DOWN";
						opening = opening * decrease;
						indexDown++;
					}
					totalCalculations++;
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid Basic calculation");
		}
		return totalCalculations;
    }
    
    public float getSpread (final String currentCurrency) {
    	float result = 0;

    	logger.debug("Data source set to: " + applicationProperties.getProperty("application.datasource"));
    	if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
    		logger.debug("Retrieving spread value for " + currentCurrency + " from database");
    		// Populate spread data from mysql database
    		result = DatabaseUtils.getSpread(currentCurrency);  		
    	} else {

    		logger.debug("Retrieving spread value for " + currentCurrency + " from file");
   	    	int counter = 0;

			String historicalDataPath = ApplicationProperties.getStringProperty("worker.historicalDataPath");
			String historicalDataFileExtension = ApplicationProperties.getStringProperty("worker.historicalDataFileExtension");
			String historicalDataSeparator = ApplicationProperties.getStringProperty("worker.historicalDataSeparator");

			String fileName = historicalDataPath + "pares" + historicalDataFileExtension;
    		        	
        	try {
        		CSVReader reader = new CSVReader(new FileReader(fileName), historicalDataSeparator.charAt(0));
    	        String [] nextLine;
    	        while ((nextLine = reader.readNext()) != null) {
    	        	counter++;
    	        	
    	        	if (currentCurrency.equals(nextLine[1])) {
    	        		result = Float.parseFloat(nextLine[2]);
    	        		break;
    	        	}
    	        	
    	       	}
    	        reader.close();
    	    	
        	} catch (Exception ex) {
        		logger.error ("Exception in file " + fileName + " - line " + counter + " - " + ex.getClass() + " - " + ex.getMessage());
        	}
    	}
    	
   		logger.info (currentCurrency + " -> Spread: " + result);

    	return result;
    }
	
	// Populates historical data and puts the objects into historical data list)
    // Depending on the datasource parameter, data could be retrieved from database (mysql) or files
    // FX Historical Data format: conversionDate,conversionTime,open,high,low,close
    public long populateHistoricalFxData (final String currentCurrency, final String startDate, final String endDate) {
    	
    	long result = 0;
    	
    	logger.debug("Data source set to: " + applicationProperties.getProperty("application.datasource"));

    	if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
    		// Populate historical data from mysql database
    		
    		historicalDataMap = DatabaseUtils.getHistoricalRates(currentCurrency, startDate, endDate);
    		
    		if (historicalDataMap != null && historicalDataMap.size() > 0) {
    			// There should be only 1 record in the map corresponding to the currentCurrency
   	            logger.info (currentCurrency + " -> total FX records loaded " + historicalDataMap.get(currentCurrency).size());
    		}
    	} else {

   	    	int totalCounter = 0;
   	    	int lineNumber = 0;

			String historicalDataPath = ApplicationProperties.getStringProperty("worker.historicalDataPath");
			String historicalDataFileExtension = ApplicationProperties.getStringProperty("worker.historicalDataFileExtension");
			String historicalDataSeparator = ApplicationProperties.getStringProperty("worker.historicalDataSeparator");
			int printAfter = ApplicationProperties.getIntProperty("worker.printAfter");

			String fileName = historicalDataPath + currentCurrency + historicalDataFileExtension;
    		
        	logger.info("Populating historical data from file (" + fileName + "). Fields separated by " + historicalDataSeparator.charAt(0));
        	
        	try {
        		CSVReader reader = new CSVReader(new FileReader(fileName), historicalDataSeparator.charAt(0));
    	        String [] nextLine;
    	        while ((nextLine = reader.readNext()) != null) {
    	        	
    	        	FxRate fxRate = new FxRate (currentCurrency,nextLine,totalCounter,startDate,endDate);
    	        	
    	        	// Check if the fxRate has been created or excluded due to the date filtering
    	        	if (currentCurrency.equals(fxRate.getCurrencyPair())) {
    					if (!historicalDataMap.containsKey(currentCurrency)) {
    						historicalDataMap.put(currentCurrency, new ArrayList<FxRate>());							
    					}
    					(historicalDataMap.get(currentCurrency)).add(fxRate);

    					if (totalCounter%printAfter == 0) {
        		        	logger.debug ("  " + currentCurrency + " -> loaded " + totalCounter + " records so far");
        				}
    					totalCounter++;
    	        	}
    	        	lineNumber++;
    	        }
    	        logger.info (currentCurrency + " -> total records loaded " + totalCounter);
    	        reader.close();
    	    	
        	} catch (Exception ex) {
        		logger.error ("Exception in file " + fileName + " - line " + lineNumber + " - " + ex.getClass() + " - " + ex.getMessage());
        	}
    	}
    	
    	if (historicalDataMap.containsKey(currentCurrency)) {
    		result = historicalDataMap.get(currentCurrency).size();
    	}
    	
    	return result;
    }

	public long getTotalBasicResults () { return this.totalBasicResults; }
	public long getTotalSpreadResults () { return this.totalSpreadResults; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalHistDataLoaded () {	return this.totalHistDataLoaded; }
	public long getElapsedTimeMillis () { return this.elapsedTimeMillis; }
}
