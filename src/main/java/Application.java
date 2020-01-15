import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        printResultsToFile ();	
        
        // Put results into Hazelcast - statusMap
        updateHazelcastResults();
        
		// Set status to Results Updated...
		HazelcastInstanceUtils.setStatus(Constants.HZ_STATUS_APPLICATION_FINSIHED);

		// Shutdown Hazelcast		
		logger.info ("Shutting down hazelcast instance...");
		HazelcastInstanceUtils.shutdown();
        
		logger.info("Application finished");
		// Exit application
		System.exit(0);
    }
    
    private static void createAndPublishExecutionTasks () throws Exception {

    	logger.info("Putting Execution Tasks into Hazelcast for processing");
    	
    	ExecutionTask executionTask = null;
    	int taskId = 0;

		// For each start and end date, currencyPair and calculation methodology (from properties file) create an Execution Task and put it into Hazelcast task queue for processing
	    for (String datePair : ApplicationProperties.getListProperty(Constants.AP_DATES)) {
	    	String startDate = datePair.substring(0,datePair.indexOf("|")).trim();
	    	String endDate = datePair.substring(datePair.indexOf("|")+1).trim();
    		for (String currentCurrency : ApplicationProperties.getListProperty(Constants.AP_CURRENCYPAIRS)) {
	        	for (String calculationType : ApplicationProperties.getListProperty(Constants.AP_CALCULATIONS)) {
		    		taskId++;
		    		logger.info ("Task created [" + taskId + "]. Parameters [" + currentCurrency + " - " + calculationType + " - " + startDate + " - " + endDate + "]");
		    		executionTask = new ExecutionTask (taskId,calculationType,currentCurrency,startDate,endDate, ApplicationProperties.getApplicationProperties());
		    		HazelcastInstanceUtils.putIntoQueue(HazelcastInstanceUtils.getTaskQueueName(), executionTask); 
	        	}
			}
	    }

    	// Set total execution task
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalTasks", taskId);
    	
        logger.info ("Created and Published " + taskId + " execution tasks");
		HazelcastInstanceUtils.putStopSignalIntoQueue(HazelcastInstanceUtils.getTaskQueueName());
		
    }

    private static void checkWorkersCompletion () throws Exception {
    	long monitorDelay = ApplicationProperties.getLongProperty(Constants.AP_MONITORDELAY);
    	
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
		logger.info ("  - datasource               : " + ApplicationProperties.getStringProperty(Constants.AP_DATASOURCE));
		logger.info ("  - database host            : " + ApplicationProperties.getStringProperty(Constants.DB_HOST));
		logger.info ("  - database port            : " + ApplicationProperties.getStringProperty(Constants.DB_PORT));
		logger.info ("  - database name            : " + ApplicationProperties.getStringProperty(Constants.DB_NAME));
		logger.info ("  - database username        : " + ApplicationProperties.getStringProperty(Constants.DB_USERNAME));
		logger.info ("  - database password        : " + ApplicationProperties.getStringProperty(Constants.DB_PASSWORD));

		logger.info ("  - currency pairs           : " + ApplicationProperties.getListProperty(Constants.AP_CURRENCYPAIRS).toString());
		logger.info ("  - dates                    : " + ApplicationProperties.getStringProperty(Constants.AP_DATES));
		logger.info ("  - increase percentage      : " + ApplicationProperties.getStringProperty(Constants.AP_INCREASEPERCENTAGE));
		logger.info ("  - decrease percentage      : " + ApplicationProperties.getStringProperty(Constants.AP_DECREASEPERCENTAGE));
		logger.info ("  - max. levels              : " + ApplicationProperties.getStringProperty(Constants.AP_MAXLEVELS));
		logger.info ("  - multiple percentages     : " + ApplicationProperties.getStringProperty(Constants.AP_MULTIPLE));
		logger.info ("  - calculations             : " + ApplicationProperties.getListProperty(Constants.AP_CALCULATIONS).toString());

		logger.info ("  - write results to file    : " + ApplicationProperties.getStringProperty(Constants.AP_WRITERESULTSTOFILE));
		logger.info ("  - results path             : " + ApplicationProperties.getStringProperty(Constants.AP_RESULTSPATH));
		logger.info ("****************************************************");
		logger.info ("");
	}
    
	// Print results to log
	private static void printResultsToLog () throws Exception {

		logger.info ("Printing results to log");
		Map<String,Integer> resultsMap = null;
		
		Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getResultsMapName()).entrySet().iterator();

		while (iter.hasNext()) {
            Entry<String, Object> entry = iter.next();

            totalExecutions++;
            totalHistDataLoaded += (((ExecutionTask) entry.getValue()).getCalculationResult()).getTotalHistoricalDataLoaded();
            totalCalculations += (((ExecutionTask) entry.getValue()).getCalculationResult()).getTotalCalculations();

            resultsMap = ((ExecutionTask) entry.getValue()).getCalculationResult().getResultsMap();

            if (resultsMap != null && resultsMap.size() > 0) {

            	logger.info (((ExecutionTask) entry.getValue()).getCurrentCurrency() + " - " + 
            				 ((ExecutionTask) entry.getValue()).getTaskType() + " - [" +
            				 ((ExecutionTask) entry.getValue()).getStartDate() + " > " +
            				 ((ExecutionTask) entry.getValue()).getEndDate() + "]");
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

		if (ApplicationProperties.getBooleanProperty(Constants.AP_WRITERESULTSTOFILE)) {
			ArrayList<String> listHeaders = new ArrayList<String>();
			Map<String,Integer> resultsMap = null;
	
			logger.info ("Printing results to results file");
			String resultsPath = ApplicationProperties.getStringProperty(Constants.AP_RESULTSPATH);
			int maxLevels = ApplicationProperties.getIntProperty(Constants.AP_MAXLEVELS);
	
			Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getResultsMapName()).entrySet().iterator();
	
			while (iter.hasNext()) {
	            Entry<String, Object> entry = iter.next();
	
	            resultsMap = ((ExecutionTask) entry.getValue()).getCalculationResult().getResultsMap();
	            resultFilePath = Paths.get(resultsPath + (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))+"_["+((ExecutionTask) entry.getValue()).getStartDate()+"_"+((ExecutionTask) entry.getValue()).getEndDate()+"].csv"));
	
	            if (resultsMap != null && resultsMap.size() > 0) {
					if (!listHeaders.contains(resultFilePath.toString())) {
						GeneralUtils.writeTextToFile(resultFilePath, ApplicationProperties.printProperties());
					}
					
					if (!(((ExecutionTask) entry.getValue()).getTaskType()).equalsIgnoreCase("SPREAD")) {
						if (!listHeaders.contains(resultFilePath.toString())) {
							GeneralUtils.writeTextToFile(resultFilePath, GeneralUtils.printResultsHeader(maxLevels));
						}
						GeneralUtils.writeTextToFile(resultFilePath, GeneralUtils.printResultsLevels (((ExecutionTask) entry.getValue()).getCurrentCurrency(), ((ExecutionTask) entry.getValue()).getTaskType(), ((ExecutionTask) entry.getValue()).getStartDate(), ((ExecutionTask) entry.getValue()).getEndDate(), resultsMap, maxLevels));
					} else {
						Iterator<Entry<String, Integer>> calcResults = resultsMap.entrySet().iterator();   						
						while (calcResults.hasNext()) {
							Entry<String, Integer> calcEntry = calcResults.next();
							GeneralUtils.writeTextToFile(resultFilePath, ((ExecutionTask) entry.getValue()).getCurrentCurrency() + "|" + ((ExecutionTask) entry.getValue()).getTaskType()  + "|" + ((ExecutionTask) entry.getValue()).getStartDate()  + "|" + ((ExecutionTask) entry.getValue()).getEndDate() + "|" + calcEntry.getKey() + "|" + calcEntry.getValue());
						}
					}
					if (!listHeaders.contains(resultFilePath.toString())) {
						listHeaders.add(resultFilePath.toString());
					}
	   			}
	        }
			if (resultsMap != null && resultsMap.size() > 0) {
				logger.info("Results written into file: " + resultFilePath.toString());
			} else {
				logger.info("No results found");
			}
		} else {
			logger.info("Print to File set to false. Results are not written into file");
		}
	}

	private static void updateHazelcastResults () throws Exception {
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalExecutions", String.format("%,d", totalExecutions));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "avgExecutionTime", GeneralUtils.printElapsedTime (0));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalHistDataLoaded", String.format("%,d", totalHistDataLoaded));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "totalCalculations", String.format("%,d", totalCalculations));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "elapsedTime", GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime));
    	HazelcastInstanceUtils.putIntoMap(HazelcastInstanceUtils.getStatusMapName(), "resultFilePath", (resultFilePath!=null)?resultFilePath.toString():"");
	}
}
