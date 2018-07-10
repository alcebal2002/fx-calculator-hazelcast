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
	private Map<String, Integer> c1212ResultsMap = new HashMap<String, Integer>();
	private Map<String, CalcResult> calcResultsMap;
	
	private long elapsedTimeMillis;
	private long totalHistDataLoaded = 0;
	private long totalCalculations = 0;
	private long totalBasicResults = 0;
	private long totalSpreadResults = 0;
	private long total1212Results = 0;

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
			
			// Calculates required properties based on the application properties retrieved from the execution task
			float increasePercentage = Float.parseFloat(applicationProperties.getProperty("application.increasePercentage"));
			float decreasePercentage = Float.parseFloat(applicationProperties.getProperty("application.decreasePercentage"));
			int maxLevels = Integer.parseInt(applicationProperties.getProperty("application.maxLevels"));
			int maxFirstIterations = Integer.parseInt(applicationProperties.getProperty("application.maxFirstIterations"));
			float spread = 0;
			
			if (checkIfCurrencyExists ()) {

				logger.info ("Populating historical data for " + currentCurrency);
				histDataStartTime = System.currentTimeMillis();
				totalHistDataLoaded = populateHistoricalFxData(currentCurrency,applicationProperties);
				histDataStopTime = System.currentTimeMillis();
				logger.info ("Historical data populated for " + currentCurrency);

				calculationStartTime = System.currentTimeMillis();
				
				if ((applicationProperties.getProperty("application.calculations")).toLowerCase().contains("basic")) {
					logger.info ("Starting basic calculations for " + currentCurrency);
					totalCalculations += executeBasicCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels);
				}
				if ((applicationProperties.getProperty("application.calculations")).toLowerCase().contains("spread")) {
					logger.info ("Retrieving spread data for " + currentCurrency);
					spread = getSpread(currentCurrency,applicationProperties);
					logger.info ("Starting spread calculations for " + currentCurrency);
					totalCalculations += executeSpreadCalculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels, spread);
				}
				if ((applicationProperties.getProperty("application.calculations")).toLowerCase().contains("1212")) {
					logger.info ("Retrieving spread data for " + currentCurrency);
					spread = getSpread(currentCurrency,applicationProperties);
					logger.info ("Starting 1212 calculations for " + currentCurrency);
					totalCalculations += execute1212Calculation (currentCurrency, increasePercentage, decreasePercentage, maxLevels, spread, maxFirstIterations);
				}
				
				calculationStopTime = System.currentTimeMillis();

				totalBasicResults = basicResultsMap.size();
				totalSpreadResults = spreadResultsMap.size();
				total1212Results = c1212ResultsMap.size();

				logger.debug ("Populating Calculation Result Map for " + currentCurrency);
				// Populates the Calculation Result Map
				calcResultsMap.put(currentCurrency, new CalcResult(currentCurrency, increasePercentage, decreasePercentage, maxLevels, maxFirstIterations, spread, histDataStartTime, histDataStopTime, totalHistDataLoaded, calculationStartTime, calculationStopTime, totalCalculations, basicResultsMap, spreadResultsMap, c1212ResultsMap));

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

	public boolean checkIfCurrencyExists () {

		boolean exists = false;

		if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
			exists = DatabaseUtils.checkCurrencyTableExists(currentCurrency,applicationProperties);
		} else {
			exists = GeneralUtils.checkIfFileExists(currentCurrency, applicationProperties);
		}
		return exists;
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
						increaseMapCounter (spreadResultsMap, result.toString());
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
    
    public void increaseMapCounter (Map<String, Integer> resultMap, final String keyString) {
		if (resultMap.containsKey(keyString)) {
			resultMap.put(keyString,resultMap.get(keyString)+1);
		} else {
			resultMap.put(keyString,1);
		}
    }
    
	// Executes calculations (levels)
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

						increaseMapCounter (basicResultsMap, ("UP-"+indexUp));

						previousFound = "UP";
						opening = opening * increase;
						indexUp++;
					} else if ((targetFxRate.getLow() < (opening * decrease)) && (indexDown <= maxLevels)) {
						if (("UP").equals(previousFound)) {
							break;
						}

						increaseMapCounter (basicResultsMap, ("DOWN-"+indexUp));

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

	// Executes calculations (levels)
    public long execute1212Calculation (final String currentCurrency, final float firstPercentage, final float secondPercentage, final int maxLevels, final float spread, final int maxFirstIterations) {
    	
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
				String previous = "";
				
				long changeCounter = 1;

				for (int i=positionId+1; i<historicalDataMap.get(currentCurrency).size(); i++) {
					targetFxRate = historicalDataMap.get(currentCurrency).get(i);
					logger.debug ("Comparing against " + targetFxRate.getCurrencyPair() + "-" + targetFxRate.getPositionId());

					// Avoid assigning all the time. Just once
					if (changeCounter == maxFirstIterations + 1) {
						selectedIncrease = secondIncrease;
				    	selectedDecrease = secondDecrease;
					}
					
					if (targetFxRate.getHigh() > (opening * selectedIncrease) - spread) {
						if (("UP").equals(previous)) {
							break;
						}
						increaseMapCounter (c1212ResultsMap, ("UP-"+changeCounter));
						
						changeCounter++;
						previous = "UP";
						opening = (opening * selectedIncrease) - spread;
					} else if (targetFxRate.getLow() < (opening * selectedDecrease) + spread) {
						if (("DOWN").equals(previous)) {
							break;
						}
						increaseMapCounter (c1212ResultsMap, ("DOWN-"+changeCounter));

						changeCounter++;
						previous = "DOWN";
						opening = (opening * selectedDecrease) + spread;
					}
					totalCalculations++;

					// No need to continue if maxLevels have been exceeded
					if (changeCounter > maxLevels) {
						break;
					}
				}
			}
		} else {
			logger.info("No historical data available for " + currentCurrency + ". Avoid 1212 calculation");
		}
		return totalCalculations;
    }
    

    public float getSpread (final String currentCurrency, final Properties applicationProperties) {
    	float result = 0;

    	logger.debug("Data source set to: " + applicationProperties.getProperty("application.datasource"));
    	if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
    		logger.debug("Retrieving spread value for " + currentCurrency + " from database");
    		// Populate spread data from mysql database
    		result = DatabaseUtils.getSpread(currentCurrency, applicationProperties);  		
    	} else {

    		logger.debug("Retrieving spread value for " + currentCurrency + " from file");
   	    	int counter = 0;

			String historicalDataPath = applicationProperties.getProperty("worker.historicalDataPath");
			String historicalDataFileExtension = applicationProperties.getProperty("worker.historicalDataFileExtension");
			String historicalDataSeparator = applicationProperties.getProperty("worker.historicalDataSeparator");

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
    public long populateHistoricalFxData (final String currentCurrency, final Properties applicationProperties) {
    	
    	long result = 0;
    	
    	logger.info("Data source set to: " + applicationProperties.getProperty("application.datasource"));

    	if ("database".equals(applicationProperties.getProperty("application.datasource"))) {
    		
    		// Gets properties from task item properties
    		
    		// Populate historical data from mysql database
    		historicalDataMap = DatabaseUtils.getHistoricalRates(currentCurrency, applicationProperties);
    		
    		if (historicalDataMap != null && historicalDataMap.size() > 0) {
    			// There should be only 1 record in the map corresponding to the currentCurrency
   	            logger.info (currentCurrency + " -> total FX records loaded " + historicalDataMap.get(currentCurrency).size());
    		}
    	} else {

   	    	int totalCounter = 0;
   	    	int lineNumber = 0;

   	    	// Gets properties from application.properties file   	    	
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
    	        	
    	        	FxRate fxRate = new FxRate (currentCurrency,nextLine,totalCounter,applicationProperties.getProperty("application.startDate"),applicationProperties.getProperty("application.endDate"));
    	        	
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
	public long getTotal1212Results () { return this.total1212Results; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalHistDataLoaded () {	return this.totalHistDataLoaded; }
	public long getElapsedTimeMillis () { return this.elapsedTimeMillis; }
}
