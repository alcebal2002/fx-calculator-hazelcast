import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datamodel.CalcResult;
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
	private static long totalBasicResults = 0;
	private static long totalSpreadResults = 0;
	private static long avgExecutionTime = 0;
		
    public static void main (String args[]) throws Exception {

    	applicationStartTime = System.currentTimeMillis();

		logger.info("Application started");

		// Load properties from file
		ApplicationProperties.loadApplicationProperties ();

		// Print parameters used
		printParameters ("Start");
		
		// Initialize Hazelcast instance
		HazelcastInstanceUtils.getInstance();
		
		// Wait until user press any key
		waitForKeyToContinue();
		
		// Create Execution Tasks and put them into Hazelacast
		createAndPublishExecutionTasks();
 
		// Wait until all the workers have finished
		checkWorkersCompletion ();
		
		applicationStopTime = System.currentTimeMillis();

		// Print parameters used
		printParameters ("Finished");
		
		// Print results
        printResults ();
        
		logger.info("Application finished");
		// Exit application
		System.exit(0);
    }


    private static void createAndPublishExecutionTasks () throws Exception {

    	logger.info("Putting Execution Tasks into Hazelcast for processing");
    	
    	ExecutionTask executionTask = null;
    	int taskId = 0;

		// For each currencyPair (from properties file) create an Execution Task and put it into Hazelcast task queue for processing
    	for (String currentCurrency : ApplicationProperties.getListProperty("application.currencyPairs")) {
    		taskId++;
    		logger.info ("Putting currency " + currentCurrency + " as taskId " + taskId);
    		executionTask = new ExecutionTask (taskId,"FXRATE",currentCurrency,ApplicationProperties.getApplicationProperties());
    		HazelcastInstanceUtils.putIntoQueue(HazelcastInstanceUtils.getTaskQueueName(), executionTask); 		
		}

        logger.info ("Created and Published " + taskId + " execution tasks");
		HazelcastInstanceUtils.putStopSignalIntoQueue(HazelcastInstanceUtils.getTaskQueueName());
		
    }

    private static void checkWorkersCompletion () throws Exception {
    	long monitorDelay = ApplicationProperties.getLongProperty("application.monitorDelay");
    	
		logger.info ("Waiting " + monitorDelay + " secs to start monitoring");
		Thread.sleep(monitorDelay*1000);
		logger.info ("Checking " + HazelcastInstanceUtils.getMonitorMapName() + " every "+monitorDelay+" secs");
		Thread.sleep(monitorDelay*1000);

		boolean stopMonitoring;

		while ( true ) {
			stopMonitoring = true;

			Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getMonitorMapName()).entrySet().iterator();

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
    
	// Print execution times
	private static void printResults () throws Exception {

		Path path = null;		
		List<String> currencyPairs = ApplicationProperties.getListProperty("application.currencyPairs");
		int maxLevels = ApplicationProperties.getIntProperty("application.maxLevels");
		boolean writeResultsToFile = ApplicationProperties.getBooleanProperty("application.writeResultsToFile");
		String resultsPath = ApplicationProperties.getStringProperty("application.resultsPath");

		Map <String,CalcResult> calcResultsMap = null;
		
		Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getMonitorMapName()).entrySet().iterator();

		int numWorkers = 0;
		
		while (iter.hasNext()) {
			numWorkers++;
            Entry<String, Object> entry = iter.next();
            calcResultsMap = ((WorkerDetail) entry.getValue()).getCalculationResults();
            totalExecutions += ((WorkerDetail) entry.getValue()).getTotalExecutions();
            totalHistDataLoaded += ((WorkerDetail) entry.getValue()).getTotalHistoricalDataLoaded();
            totalCalculations += ((WorkerDetail) entry.getValue()).getTotalCalculations();
            totalBasicResults += ((WorkerDetail) entry.getValue()).getTotalBasicResults();
            totalSpreadResults += ((WorkerDetail) entry.getValue()).getTotalSpreadResults();
            avgExecutionTime += ((WorkerDetail) entry.getValue()).getAvgExecutionTime();
            avgExecutionTime = avgExecutionTime / numWorkers;
            
    		logger.info ("");
    		logger.info ("Total figures:");
    		logger.info ("**************************************************");
    		logger.info ("  - Total executions         : " + String.format("%,d", totalExecutions));
    		logger.info ("  - Avg. execution time      : " + GeneralUtils.printElapsedTime (avgExecutionTime));
    		logger.info ("  - Total historical data    : " + String.format("%,d", totalHistDataLoaded));
    		logger.info ("  - Total calculations       : " + String.format("%,d", totalCalculations)); 
    		logger.info ("  - Total basic results      : " + String.format("%,d", totalBasicResults));
    		logger.info ("  - Total spread results     : " + String.format("%,d", totalSpreadResults));
    		logger.info ("  - Elapsed time             : " + GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime));
    		logger.info ("**************************************************");
    		logger.info ("");
    		logger.info ("Results:");
    		logger.info ("**************************************************");

    		if (calcResultsMap != null && calcResultsMap.size() > 0) {
     			
    			logger.info (printBasicResultsHeader(maxLevels));
    			
    			if (writeResultsToFile) {
    				path = Paths.get(resultsPath + (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))+".csv"));
    				GeneralUtils.writeTextToFile(path, printExecutionParams());
    				GeneralUtils.writeTextToFile(path, printBasicResultsHeader(maxLevels));
    			}
    			
    			for (String currency : currencyPairs) {
    				
    				if (calcResultsMap.containsKey(currency)) {
    					logger.info (printBasicResultsLevels (currency, ((CalcResult)calcResultsMap.get(currency)).getBasicResults(), maxLevels));
       					logger.info (((CalcResult)calcResultsMap.get(currency)).getSpreadResults().toString());
       				    					
    					if (writeResultsToFile) {
    						GeneralUtils.writeTextToFile(path, printBasicResultsLevels (currency, ((CalcResult)calcResultsMap.get(currency)).getBasicResults(), maxLevels));
    						GeneralUtils.writeTextToFile(path, ((CalcResult)calcResultsMap.get(currency)).getSpreadResults().toString());
    					}
    				} else {
    					logger.info (printBasicResultsLevels (currency, null, maxLevels));
    					if (writeResultsToFile) {
    						GeneralUtils.writeTextToFile(path, printBasicResultsLevels (currency, null, maxLevels));
    					}
    				}
    			}
    			logger.info ("**************************************************");
    			logger.info("");
    			if (writeResultsToFile) {
    				logger.info("Results written into file: " + path.toString());
    			}
    		}            
        }
	}

	// Print execution parameters
	private static String printExecutionParams() {
		
		List<String> currencyPairs = (List<String>)ApplicationProperties.getListProperty("application.currencyPairs");
		List<String> calculations = (List<String>)ApplicationProperties.getListProperty("application.calculations");
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
		stringBuilder.append("avg. execution time|"+GeneralUtils.printElapsedTime (avgExecutionTime)+"\n");
		stringBuilder.append("total historical data|"+String.format("%,d", totalHistDataLoaded)+"\n");
		stringBuilder.append("total calculations|"+String.format("%,d", totalCalculations)+"\n"); 
		stringBuilder.append("total basic results|"+String.format("%,d", totalBasicResults)+"\n");
		stringBuilder.append("total spread results|"+String.format("%,d", totalSpreadResults)+"\n");
		stringBuilder.append("elapsed time|"+GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime)+"\n");

		return (stringBuilder.toString());
	}

	// Print currency levels header
	private static String printBasicResultsHeader(final int maxLevels) {
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("CURRENCYPAIR");
		
		for (int i=1; i <= maxLevels; i++) {
			stringBuilder.append("|"+i+"-UP|"+i+"-DOWN|"+i+"-TOTAL|"+i+"-%");
		}
		
		return (stringBuilder.toString());
	}
	
	private static void waitForKeyToContinue () {
		logger.info ("");
		logger.info ("*************************************************************************");
		logger.info ("Ensure all the Workers are up & running. Then Press Any Key to continue...");
		logger.info ("*************************************************************************");

		try {
			System.in.read();
		} catch(Exception e) {
			logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
		}
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
}
