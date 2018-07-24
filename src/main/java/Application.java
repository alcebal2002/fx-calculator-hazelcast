import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datamodel.CalculationResult;
import datamodel.ExecutionTask;
import datamodel.WorkerDetail;
import utils.ApplicationProperties;
import utils.Constants;
import utils.GeneralUtils;
import utils.HazelcastInstanceUtils;

public class Application {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(Application.class);

	// Execution time
	private static long applicationStartTime;	
	private static long applicationStopTime;	
	
	private static long totalExecutions = 0;
	private static long totalHistDataLoaded = 0;
	private static long totalCalculations = 0;
	
	private static Path resultFilePath = null;		

		
    public static void main (String args[]) throws Exception {

    	applicationStartTime = System.currentTimeMillis();

		logger.info("Application started");

		// Load properties from file
		ApplicationProperties.loadApplicationProperties ();

		// Print parameters used
		printParameters ("Start");
		
		// Check if result directory exists if application.writeResultsToFile is set to true
		GeneralUtils.checkResultsPath();
		
		// Initialize Hazelcast instance
		HazelcastInstanceUtils.getInstance();

		// Set status to Starting...
		HazelcastInstanceUtils.setStatus(Constants.HZ_STATUS_STARTING_APPLICATION);
		
		// Wait until user press any key
		GeneralUtils.waitForKeyToContinue();
		
		// Set status to Publishing tasks...
		HazelcastInstanceUtils.setStatus(Constants.HZ_STATUS_PUBLISHING_TASKS);

		// Create Execution Tasks and put them into Hazelacast
		createAndPublishExecutionTasks();
 
		// Set status to Waiting for workers to finish...
		HazelcastInstanceUtils.setStatus(Constants.HZ_STATUS_WAITING_WORKERS);

		// Wait until all the workers have finished
		checkWorkersCompletion ();
		
		// Set status to Process completed...
		HazelcastInstanceUtils.setStatus(Constants.HZ_STATUS_PROCESS_COMPLETED);

		applicationStopTime = System.currentTimeMillis();

		// Print parameters used
		printParameters ("Finished");
		
		// Print results in the log
        printResultsToLog ();
        
		// Print results in the output file
//        printResultsToFile ();	
        
        // Put results into Hazelcast - statusMap
        updateHazelcastResults();
        
		logger.info("Application finished");
		// Exit application
		System.exit(0);
    }
    
    private static void createAndPublishExecutionTasks () throws Exception {

    	logger.info("Putting Execution Tasks into Hazelcast for processing");
    	
    	ExecutionTask executionTask = null;
    	int taskId = 0;

		// For each currencyPair and calculation methodology (from properties file) create an Execution Task and put it into Hazelcast task queue for processing
    	for (String currentCurrency : ApplicationProperties.getListProperty("application.currencyPairs")) {
        	for (String calculation : ApplicationProperties.getListProperty("application.calculations")) {
	    		taskId++;
	    		logger.info ("Putting currency " + currentCurrency + " - " + calculation + " as taskId " + taskId);
	    		executionTask = new ExecutionTask (taskId,calculation,currentCurrency,ApplicationProperties.getApplicationProperties());
	    		HazelcastInstanceUtils.putIntoQueue(HazelcastInstanceUtils.getTaskQueueName(), executionTask); 
        	}
		}

    	// Set total execution task
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalTasks", taskId);
    	
        logger.info ("Created and Published " + taskId + " execution tasks");
		HazelcastInstanceUtils.putStopSignalIntoQueue(HazelcastInstanceUtils.getTaskQueueName());
		
    }

    private static void checkWorkersCompletion () throws Exception {
    	long monitorDelay = ApplicationProperties.getLongProperty("application.monitorDelay");
    	
		logger.info ("Waiting " + monitorDelay + " secs to start monitoring");
		Thread.sleep(monitorDelay*1000);
		logger.info ("Checking " + HazelcastInstanceUtils.getWorkersMapName() + " every " + monitorDelay + " secs");
		Thread.sleep(monitorDelay*1000);

		boolean stopMonitoring;

		while ( true ) {
			stopMonitoring = true;

			Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getWorkersMapName()).entrySet().iterator();

			while (iter.hasNext()) {
	            Entry<String, Object> entry = iter.next();
	            if (((WorkerDetail) entry.getValue()).getActiveStatus()) stopMonitoring = false;
	        }
			
			if (stopMonitoring) {
				logger.info ("All clients are inactive. Stopping monitoring...");
				break;
			} else {
				logger.info ("Keeping the monitoring running every " + monitorDelay + " secs until all the clients are inactive...");
				Thread.sleep(monitorDelay*1000);
			}
		}
    }    

	// Print execution parameters 
	private static void printParameters (final String title) {
		logger.info ("");
		logger.info ("****************************************************"); 
		logger.info (title + " FXCalculator with the following parameters:"); 
		logger.info ("****************************************************"); 
		logger.info ("  - datasource               : " + ApplicationProperties.getStringProperty("application.datasource"));
		logger.info ("  - database host            : " + ApplicationProperties.getStringProperty("database.host"));
		logger.info ("  - database port            : " + ApplicationProperties.getStringProperty("database.port"));
		logger.info ("  - database name            : " + ApplicationProperties.getStringProperty("database.db_name"));
		logger.info ("  - database username        : " + ApplicationProperties.getStringProperty("database.username"));
		logger.info ("  - database password        : " + ApplicationProperties.getStringProperty("database.password"));

		logger.info ("  - currency pairs           : " + ApplicationProperties.getListProperty("application.currencyPairs").toString());
		logger.info ("  - start date               : " + ApplicationProperties.getStringProperty("application.startDate"));
		logger.info ("  - end date                 : " + ApplicationProperties.getStringProperty("application.endDate"));
		logger.info ("  - increase percentage      : " + ApplicationProperties.getStringProperty("application.increasePercentage"));
		logger.info ("  - decrease percentage      : " + ApplicationProperties.getStringProperty("application.decreasePercentage"));
		logger.info ("  - max. levels              : " + ApplicationProperties.getStringProperty("application.maxLevels"));
		logger.info ("  - calculations             : " + ApplicationProperties.getListProperty("application.calculations").toString());

		logger.info ("  - write results to file    : " + ApplicationProperties.getStringProperty("application.writeResultsToFile"));
		logger.info ("  - results path             : " + ApplicationProperties.getStringProperty("application.resultsPath"));
		logger.info ("****************************************************");
		logger.info ("");
	}
    
	// Print results to log
	private static void printResultsToLog () throws Exception {

		Map<String,Integer> resultsMap = null;
		
		Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getResultsMapName()).entrySet().iterator();

		while (iter.hasNext()) {
            Entry<String, Object> entry = iter.next();

            totalExecutions++;
            totalHistDataLoaded += (((ExecutionTask) entry.getValue()).getCalculationResult()).getTotalHistoricalDataLoaded();
            totalCalculations += (((ExecutionTask) entry.getValue()).getCalculationResult()).getTotalCalculations();

            resultsMap = ((ExecutionTask) entry.getValue()).getCalculationResult().getResultsMap();

            if (resultsMap != null && resultsMap.size() > 0) {

            	//logger.info (printBasicResultsHeader(maxLevels));
            	logger.info (resultsMap.toString());
   			}
        }
		logger.info ("");
		logger.info ("Total figures:");
		logger.info ("**************************************************");
		logger.info ("  - Total executions         : " + String.format("%,d", totalExecutions));
		logger.info ("  - Total historical data    : " + String.format("%,d", totalHistDataLoaded));
		logger.info ("  - Total calculations       : " + String.format("%,d", totalCalculations)); 
		logger.info ("  - Elapsed time             : " + GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime));
		logger.info ("**************************************************");
		logger.info ("");
	}

	// Print results to file
	private static void printResultsToFile () throws Exception {

        if (ApplicationProperties.getBooleanProperty("application.writeResultsToFile")) {
	
			List<String> currencyPairs = ApplicationProperties.getListProperty("application.currencyPairs");
			int maxLevels = ApplicationProperties.getIntProperty("application.maxLevels");
			String resultsPath = ApplicationProperties.getStringProperty("application.resultsPath");
	
			Map <String,CalculationResult> calcResultsMap = null;
			
			Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getWorkersMapName()).entrySet().iterator();
	
			int numWorkers = 0;
			
			while (iter.hasNext()) {
				numWorkers++;
	            Entry<String, Object> entry = iter.next();
/*
	            calcResultsMap = ((WorkerDetail) entry.getValue()).getCalculationResults();
	            
	    		if (calcResultsMap != null && calcResultsMap.size() > 0) {
	
    				if (numWorkers == 1) {
    					resultFilePath = Paths.get(resultsPath + (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))+".csv"));
    					GeneralUtils.writeTextToFile(resultFilePath, printExecutionParams());
    				}
    				GeneralUtils.writeTextToFile(resultFilePath, ((WorkerDetail) entry.getValue()).getInetAddres() + ":" + ((WorkerDetail) entry.getValue()).getInetPort() + " - Basic calculation results");
    				GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsHeader(maxLevels));
	
	    			// Print basic calculation results
	    			for (String currency : currencyPairs) {
	    				
	    				if (calcResultsMap.containsKey(currency)) {
	    					GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, ((CalcResult)calcResultsMap.get(currency)).getBasicResults(), maxLevels));
	    				} else {
    						GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, null, maxLevels));
	    				}
	    			}

	    			GeneralUtils.writeTextToFile(resultFilePath, ((WorkerDetail) entry.getValue()).getInetAddres() + ":" + ((WorkerDetail) entry.getValue()).getInetPort() + " - Spread calculation results");

	    			// Print spread calculation results
	    			for (String currency : currencyPairs) {
	    				
	    				if (calcResultsMap.containsKey(currency)) {
    						Iterator<Entry<String, Integer>> calcResults = ((CalcResult)calcResultsMap.get(currency)).getSpreadResults().entrySet().iterator();   						
    						while (calcResults.hasNext()) {
    							Entry<String, Integer> calcEntry = calcResults.next();
       							GeneralUtils.writeTextToFile(resultFilePath, currency + "|" + calcEntry.getKey() + "|" + calcEntry.getValue());
    						}
	    				}
	    			}
	    			
    				GeneralUtils.writeTextToFile(resultFilePath, ((WorkerDetail) entry.getValue()).getInetAddres() + ":" + ((WorkerDetail) entry.getValue()).getInetPort() + " - 1212 calculation results");
    				GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsHeader(maxLevels));
	
	    			// Print 1212 calculation results
	    			for (String currency : currencyPairs) {
	    				
	    				if (calcResultsMap.containsKey(currency)) {
	    					GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, ((CalcResult)calcResultsMap.get(currency)).get1212Results(), maxLevels));
	    				} else {
    						GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, null, maxLevels));
	    				}
	    			}

    				GeneralUtils.writeTextToFile(resultFilePath, ((WorkerDetail) entry.getValue()).getInetAddres() + ":" + ((WorkerDetail) entry.getValue()).getInetPort() + " - 1234 calculation results");
    				GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsHeader(maxLevels));
	
	    			// Print 1234 calculation results
	    			for (String currency : currencyPairs) {
	    				
	    				if (calcResultsMap.containsKey(currency)) {
	    					GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, ((CalcResult)calcResultsMap.get(currency)).get1234Results(), maxLevels));
	    				} else {
    						GeneralUtils.writeTextToFile(resultFilePath, printBasicResultsLevels (currency, null, maxLevels));
	    				}
	    			}

	    		}            
*/
	        }
			logger.info("Results written into file: " + resultFilePath.toString());
        }
	}
	
	// Print execution parameters
	private static String printExecutionParams() {
		
		List<String> currencyPairs = ApplicationProperties.getListProperty("application.currencyPairs");
		List<String> calculations = ApplicationProperties.getListProperty("application.calculations");
		String startDate = ApplicationProperties.getStringProperty("application.startDate");
		String endDate = ApplicationProperties.getStringProperty("application.endDate");
		int maxLevels = ApplicationProperties.getIntProperty("application.maxLevels");
		float increasePercentage = ApplicationProperties.getFloatProperty("application.increasePercentage");
		float decreasePercentage = ApplicationProperties.getFloatProperty("application.decreasePercentage");
		
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("currency pairs|"+currencyPairs.toString()+"\n");
		stringBuilder.append("start date|"+startDate+"\n");
		stringBuilder.append("end date|"+endDate+"\n");
		stringBuilder.append("increase percentage|"+increasePercentage+"\n");
		stringBuilder.append("decrease percentage|"+decreasePercentage+"\n");
		stringBuilder.append("max. levels|"+maxLevels+"\n");
		stringBuilder.append("calculations|"+calculations+"\n");
		stringBuilder.append("Results"+"\n");
		stringBuilder.append("total executions|"+String.format("%,d", totalExecutions)+"\n");
		stringBuilder.append("avg. execution time|BLANK"+"\n");
		stringBuilder.append("total historical data|"+String.format("%,d", totalHistDataLoaded)+"\n");
		stringBuilder.append("total calculations|"+String.format("%,d", totalCalculations)+"\n");
		
		// Iterate through the calculation methodologies and print the results 
		
		stringBuilder.append("total basic results|"+String.format("%,d", "BLANK")+"\n");
		stringBuilder.append("total spread results|"+String.format("%,d", "BLANK")+"\n");
		stringBuilder.append("total 1212 results|"+String.format("%,d", "BLANK")+"\n");
		stringBuilder.append("total 1234 results|"+String.format("%,d", "BLANK")+"\n");
		stringBuilder.append("elapsed time|"+GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime)+"\n");

		return (stringBuilder.toString());
	}

/*
	// Print currency levels header
	private static String printBasicResultsHeader(final int maxLevels) {
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("CURRENCYPAIR");
		
		for (int i=1; i <= maxLevels; i++) {
			stringBuilder.append("|"+i+"-UP|"+i+"-DOWN|"+i+"-TOTAL|"+i+"-%");
		}
		
		return (stringBuilder.toString());
	}
	
	// Print currency result levels
	private static String printBasicResultsLevels (final String currency, final Map<String,Integer> levelsMap, final int maxLevels) {
		
		StringBuilder stringBuilder = new StringBuilder();
		
		double referenceLevel = 0;
		
		for (int i=1; i <= maxLevels; i++) {
			long total=0;
			if (levelsMap != null && levelsMap.containsKey("UP-"+i)) {
				stringBuilder.append(levelsMap.get("UP-"+i));
				total += levelsMap.get("UP-"+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			if (levelsMap != null && levelsMap.containsKey("DOWN-"+i)) {
				stringBuilder.append(levelsMap.get("DOWN-"+i));
				total += levelsMap.get("DOWN-"+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			stringBuilder.append(total);
			stringBuilder.append("|");
			if (i==1) referenceLevel = total;

			if (total == 0) {
				stringBuilder.append("0");
			} else {
				stringBuilder.append(new DecimalFormat("#.##").format(total*100/referenceLevel));
			}
			stringBuilder.append("|");
		}
		
		return (currency + "|" + stringBuilder.toString());
	}
*/

/*	
	// Print currency result levels
	private static String print1212ResultsLevels (final String currency, final Map<String,Integer> levelsMap, final int maxLevels) {
		
		StringBuilder stringBuilder = new StringBuilder();
		
		double referenceLevel = 0;
		
		for (int i=1; i <= maxLevels; i++) {
			long total=0;
			if (levelsMap != null && levelsMap.containsKey(""+i)) {
				stringBuilder.append(levelsMap.get(""+i));
				total += levelsMap.get(""+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			if (levelsMap != null && levelsMap.containsKey(""+i)) {
				stringBuilder.append(levelsMap.get(""+i));
				total += levelsMap.get(""+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			stringBuilder.append(total);
			stringBuilder.append("|");
			if (i==1) referenceLevel = total;

			if (total == 0) {
				stringBuilder.append("0");
			} else {
				stringBuilder.append(new DecimalFormat("#.##").format(total*100/referenceLevel));
			}
			stringBuilder.append("|");
		}
		
		return (currency + "|" + stringBuilder.toString());		
	}
*/
	
	private static void updateHazelcastResults () throws Exception {
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalExecutions", String.format("%,d", totalExecutions));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "avgExecutionTime", GeneralUtils.printElapsedTime (0));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalHistDataLoaded", String.format("%,d", totalHistDataLoaded));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalCalculations", String.format("%,d", totalCalculations));

    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "elapsedTime", GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime));
//    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "resultFilePath", resultFilePath.toString());
	}
}
