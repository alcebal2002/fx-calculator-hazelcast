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
import utils.GeneralUtils;
import utils.HazelcastInstanceUtils;

public class Application {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(Application.class);

	// Execution time
	private static long applicationStartTime;	
	private static long applicationStopTime;	
	
	// Application properties
	private static Properties applicationProperties = new Properties();
/*
	private static String historicalDataPath;
	private static String historicalDataFileExtension;
	private static String historicalDataSeparator;
	private static int printAfter = 0;
	private static boolean writeResultsToFile = false; 
	private static String resultsPath;
	
	private static String datasource;

	private static String databaseHost;
	private static String databasePort;
	private static String databaseName;
	private static String databaseUser;
	private static String databasePass;
	
	private static List<String> currencyPairs;
	private static String startDate;
	private static String endDate;
	private static float increasePercentage;
	private static float decreasePercentage;
	private static int maxLevels;
*/
	private static long totalExecutions = 0;
	private static long totalHistDataLoaded = 0;
	private static long totalCalculations = 0;
	private static long totalResults = 0;
	private static long avgExecutionTime = 0;
		
    public static void main (String args[]) throws Exception {

    	applicationStartTime = System.currentTimeMillis();

		logger.info("Application started");
		logger.info("Loading application properties from " + ApplicationProperties.getPropertiesFile());

		// Load properties from file
		loadProperties ();
    	
		// Print parameters used
		printParameters ("Start");
		
		// Initialize Hazelcast instance
		HazelcastInstanceUtils.getInstance();
		
		// Create Execution Tasks and put them into Hazelacast
		createAndPublishExecutionTasks();
 
		checkWorkersCompletion ();
		
		applicationStopTime = System.currentTimeMillis();

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
    	for (String currentCurrency : ((List<String>)applicationProperties.get("execution.currencyPairs"))) {
    		taskId++;
    		logger.info ("Putting currency " + currentCurrency + " as taskId " + taskId);
    		executionTask = new ExecutionTask (taskId,"FXRATE",currentCurrency,applicationProperties);
    		HazelcastInstanceUtils.putIntoQueue(HazelcastInstanceUtils.getTaskQueueName(), executionTask); 		
		}

        logger.info ("Created and Published " + taskId + " execution tasks");
		HazelcastInstanceUtils.putStopSignalIntoQueue(HazelcastInstanceUtils.getTaskQueueName());
		
    }

    private static void checkWorkersCompletion () throws Exception {
    	long monitorDelay = Long.parseLong(applicationProperties.getProperty("main.monitorDelay"));
    	
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
    

    private static void loadProperties () {
    	
    	applicationProperties.put("main.historicalDataPath", ApplicationProperties.getStringProperty("main.historicalDataPath"));
    	applicationProperties.put("main.historicalDataFileExtension", ApplicationProperties.getStringProperty("main.historicalDataFileExtension"));
    	applicationProperties.put("main.historicalDataSeparator", ApplicationProperties.getStringProperty("main.historicalDataSeparator"));
    	applicationProperties.put("main.writeResultsToFile", ApplicationProperties.getBooleanProperty("main.writeResultsToFile"));
    	applicationProperties.put("main.resultsPath", ApplicationProperties.getStringProperty("main.resultsPath"));
    	applicationProperties.put("main.monitorDelay", ApplicationProperties.getStringProperty("main.monitorDelay"));
		
    	applicationProperties.put("main.datasource", ApplicationProperties.getStringProperty("main.datasource"));
    	applicationProperties.put("database.host", ApplicationProperties.getStringProperty("database.host"));
    	applicationProperties.put("database.port", ApplicationProperties.getStringProperty("database.port"));
    	applicationProperties.put("main.historicalDataPath", ApplicationProperties.getStringProperty("database.db_name"));
    	applicationProperties.put("database.username", ApplicationProperties.getStringProperty("database.username"));
    	applicationProperties.put("database.password", ApplicationProperties.getStringProperty("database.password"));

    	applicationProperties.put("execution.currencyPairs", ApplicationProperties.getListProperty("execution.currencyPairs"));
    	applicationProperties.put("execution.startDate", ApplicationProperties.getStringProperty("execution.startDate"));
    	applicationProperties.put("execution.endDate", ApplicationProperties.getStringProperty("execution.endDate"));
    	applicationProperties.put("execution.increasePercentage", ApplicationProperties.getFloatProperty("execution.increasePercentage"));
    	applicationProperties.put("execution.decreasePercentage", ApplicationProperties.getFloatProperty("execution.decreasePercentage"));
    	applicationProperties.put("execution.maxLevels", ApplicationProperties.getIntProperty("execution.maxLevels"));
    	applicationProperties.put("test.printAfter", ApplicationProperties.getIntProperty("test.printAfter"));

    }
    
	// Print execution parameters 
	private static void printParameters (final String title) {
		
/*		
		logger.info ("");
		logger.info ("**************************************************"); 
		logger.info (title + " WorkerPool with the following parameters:"); 
		logger.info ("**************************************************"); 
		logger.info ("  - pool core size           : " + poolCoreSize); 
		logger.info ("  - pool max size            : " + poolMaxSize); 
		logger.info ("  - queue capacity           : " + queueCapacity); 
		logger.info ("  - timeout (secs)           : " + timeoutSecs); 
		logger.info ("  - retry sleep (ms)         : " + retrySleepTime); 
		logger.info ("  - retry max attempts       : " + retryMaxAttempts);
		logger.info ("  - initial sleep (secs)     : " + initialSleep); 
		logger.info ("  - monitor sleep (secs)     : " + monitorSleep); 
		logger.info ("**************************************************");

		logger.info ("");
		logger.info ("****************************************************"); 
		logger.info (title + " FXCalculator with the following parameters:"); 
		logger.info ("****************************************************"); 
		logger.info ("  - datasource               : " + datasource);
		logger.info ("  - hist. data path          : " + historicalDataPath);
		logger.info ("  - hist. data extension     : " + historicalDataFileExtension);
		logger.info ("  - hist. data separator     : " + historicalDataSeparator);

		logger.info ("  - database host            : " + databaseHost);
		logger.info ("  - database port            : " + databasePort);
		logger.info ("  - database name            : " + databaseName);
		logger.info ("  - database username        : " + databaseUser);
		logger.info ("  - database password        : " + databasePass);

		logger.info ("  - currency pairs           : " + currencyPairs.toString());
		logger.info ("  - start date               : " + startDate);
		logger.info ("  - end date                 : " + endDate);
		logger.info ("  - increase percentage      : " + increasePercentage);
		logger.info ("  - decrease percentage      : " + decreasePercentage);
		logger.info ("  - max. levels              : " + maxLevels);
		logger.info ("  - number of records [test] : " + numberOfRecords); 
		logger.info ("  - print after [test]       : " + printAfter);

		logger.info ("  - write results to file    : " + writeResultsToFile);
		logger.info ("  - results path             : " + resultsPath);
		logger.info ("****************************************************");
		logger.info ("");
*/
	}

	// Print execution times
	private static void printResults () throws Exception {

		Path path = null;		
		List<String> currencyPairs = (List<String>)applicationProperties.get("execution.currencyPairs");
		int maxLevels = (int)applicationProperties.get("execution.maxLevels");
		boolean writeResultsToFile = (boolean)applicationProperties.get("main.writeResultsToFile");
		String resultsPath = (String)applicationProperties.get("main.resultsPath");

		Map <String,CalcResult> calcResultsMap = null;
		
		Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getMonitorMapName()).entrySet().iterator();

		while (iter.hasNext()) {
            Entry<String, Object> entry = iter.next();
            calcResultsMap = ((WorkerDetail) entry.getValue()).getCalculationResults();
            totalExecutions += ((WorkerDetail) entry.getValue()).getTotalExecutions();
            totalHistDataLoaded += ((WorkerDetail) entry.getValue()).getTotalHistoricalDataLoaded();
            totalCalculations += ((WorkerDetail) entry.getValue()).getTotalCalculations();
            totalResults += ((WorkerDetail) entry.getValue()).getTotalResults();
            
    		if (calcResultsMap != null && calcResultsMap.size() > 0) {
     			
    			logger.info (printCurrencyLevelsHeader(maxLevels));
    			
    			if (writeResultsToFile) {
    				path = Paths.get(resultsPath + (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))+".csv"));
    				GeneralUtils.writeTextToFile(path, printExecutionParams());
    				GeneralUtils.writeTextToFile(path, printCurrencyLevelsHeader(maxLevels));
    			}
    			
    			for (String currency : currencyPairs) {
    				
    				if (calcResultsMap.containsKey(currency)) {
    					logger.info (printCurrencyLevels (currency, ((CalcResult)calcResultsMap.get(currency)).getLevelResults(), maxLevels));
    					
    					if (writeResultsToFile) {
    						GeneralUtils.writeTextToFile(path, printCurrencyLevels (currency, ((CalcResult)calcResultsMap.get(currency)).getLevelResults(), maxLevels));
    					}
    				} else {
    					logger.info (printCurrencyLevels (currency, null, maxLevels));
    					if (writeResultsToFile) {
    						GeneralUtils.writeTextToFile(path, printCurrencyLevels (currency, null, maxLevels));
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
		logger.info ("");
		logger.info ("Total figures:");
		logger.info ("**************************************************");
		logger.info ("  - Total executions         : " + String.format("%,d", totalExecutions));
		logger.info ("  - Avg. execution time      : " + GeneralUtils.printElapsedTime (avgExecutionTime));
		logger.info ("  - Total historical data    : " + String.format("%,d", totalHistDataLoaded));
		logger.info ("  - Total calculations       : " + String.format("%,d", totalCalculations)); 
		logger.info ("  - Total results            : " + String.format("%,d", totalResults));
		logger.info ("  - Elapsed time             : " + GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime));
		logger.info ("**************************************************");
		logger.info ("");
		logger.info ("Results:");
		logger.info ("**************************************************");
	}

	// Print execution parameters
	private static String printExecutionParams() {
		
		List<String> currencyPairs = (List<String>)ApplicationProperties.getListProperty("execution.currencyPairs");
		String startDate = ApplicationProperties.getStringProperty("execution.startDate");
		String endDate = ApplicationProperties.getStringProperty("execution.endDate");
		int maxLevels = (int)applicationProperties.get("execution.maxLevels");
		float increasePercentage = ApplicationProperties.getFloatProperty("execution.increasePercentage");
		float decreasePercentage = ApplicationProperties.getFloatProperty("execution.decreasePercentage");
		
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("currency pairs|"+currencyPairs.toString()+"\n");
		stringBuilder.append("start date|"+startDate+"\n");
		stringBuilder.append("end date|"+endDate+"\n");
		stringBuilder.append("increase percentage|"+increasePercentage+"\n");
		stringBuilder.append("decrease percentage|"+decreasePercentage+"\n");
		stringBuilder.append("max. levels|"+maxLevels+"\n");
		stringBuilder.append("Results"+"\n");
		stringBuilder.append("total executions|"+String.format("%,d", totalExecutions)+"\n");
		stringBuilder.append("avg. execution time|"+GeneralUtils.printElapsedTime (avgExecutionTime)+"\n");
		stringBuilder.append("total historical data|"+String.format("%,d", totalHistDataLoaded)+"\n");
		stringBuilder.append("total calculations|"+String.format("%,d", totalCalculations)+"\n"); 
		stringBuilder.append("total results|"+String.format("%,d", totalResults)+"\n");
		stringBuilder.append("elapsed time|"+GeneralUtils.printElapsedTime (applicationStartTime,applicationStopTime)+"\n");

		return (stringBuilder.toString());
	}

	// Print currency levels header
	private static String printCurrencyLevelsHeader(final int maxLevels) {
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("CURRENCYPAIR");
		
		for (int i=1; i <= maxLevels; i++) {
			stringBuilder.append("|"+i+"-UP|"+i+"-DOWN|"+i+"-TOTAL|"+i+"-%");
		}
		
		return (stringBuilder.toString());
	}
	
	// Print currency result levels
	private static String printCurrencyLevels (final String currency, final Map<String,Integer> levelsMap, final int maxLevels) {
		
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
