import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

import datamodel.CalcResult;
import datamodel.ExecutionTask;
import datamodel.WorkerDetail;
import executionservices.RejectedExecutionHandlerImpl;
import executionservices.RunnableWorkerThread;
import executionservices.SystemLinkedBlockingQueue;
import executionservices.SystemMonitorThread;
import executionservices.SystemThreadPoolExecutor;
import utils.ApplicationProperties;
import utils.GeneralUtils;
import utils.HazelcastInstanceUtils;

public class Worker {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(Worker.class);
	
	// Hazelcast client
	private static HazelcastInstance hzClient;
	
	// Worker parameters 
	private static int poolCoreSize;
	private static int poolMaxSize; 
	private static int queueCapacity; 
	private static int timeoutSecs; 
	private static int retrySleepTime; 
	private static int retryMaxAttempts; 
	private static int initialSleep; 
	private static int monitorSleep;
	
	private static String nodeId;
	private static String localEndPointAddress;
	private static String localEndPointPort;
	
	private static long totalExecutions = 0;
	
	private static Map<String,CalcResult> calcResultsMap = new HashMap<String,CalcResult>();
	
	public static void main(String args[]) throws Exception {
		
		logger.info("WorkerPool started");
		
		// Load worker properties
		loadWorkerProperties();
		
		logger.info ("Waiting " + initialSleep + " secs to start..."); 
		Thread.sleep(initialSleep*1000); 

		printParameters ("Started");

		// Create hazelcast client. Retry connection <retryMaxAttempts> times every <retrySleepTime> seconds
		logger.info("Create Hazelcast client instance");
		if (createHazelcastClientInstance()) {
			// RejectedExecutionHandler implementation 
			RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl(); 
			
			// Get the ThreadFactory implementation to use 
			ThreadFactory threadFactory = Executors.defaultThreadFactory();
			
			/* Define the BlockingQueue. 
			 * ArrayBlockingQueue to set a fixed capacity queue
			 * LinkedBlockingQueue to set an unbound capacity queue
			*/
			SystemLinkedBlockingQueue<Runnable> blockingQueue = new SystemLinkedBlockingQueue<Runnable>();		
			
			// Creating the ThreadPoolExecutor 
			SystemThreadPoolExecutor executorPool = new SystemThreadPoolExecutor(poolCoreSize, poolMaxSize, timeoutSecs, TimeUnit.SECONDS, blockingQueue, threadFactory, rejectionHandler); 
			
			// Create cluster node object
			long startTime = System.currentTimeMillis();
					
			nodeId = ""+System.currentTimeMillis();
			localEndPointAddress = hzClient.getLocalEndpoint().getSocketAddress().toString();
			localEndPointPort = localEndPointAddress.substring(localEndPointAddress.indexOf(":")+1);
			localEndPointAddress = localEndPointAddress.substring(1,localEndPointAddress.indexOf(":"));
			
			WorkerDetail workerDetail = new WorkerDetail(
					nodeId,
					GeneralUtils.getHostName(),
					localEndPointPort,
					poolCoreSize,
					poolMaxSize,
					queueCapacity,
					timeoutSecs,
					retrySleepTime,
					retryMaxAttempts,
					initialSleep,
					monitorSleep,
					startTime);

			// Start the monitoring thread 
			SystemMonitorThread monitor = new SystemMonitorThread(executorPool, monitorSleep, nodeId); 
			Thread monitorThread = new Thread(monitor); 
			monitorThread.start(); 

			hzClient.getMap(HazelcastInstanceUtils.getMonitorMapName()).put(workerDetail.getUuid(),workerDetail);
			
			// Listen to Hazelcast tasks queue and submit work to the thread pool for each task 
			IQueue<ExecutionTask> hazelcastTaskQueue = hzClient.getQueue( HazelcastInstanceUtils.getTaskQueueName() );
					
			while ( true ) {
				/*
				 * Option to avoid getting additional tasks from Hazelcast distributed queue if there is no processing capacity available in the ThreadPool 
				 */
				if ((executorPool.getActiveCount() < executorPool.getMaximumPoolSize()) ||
//					(blockingQueue.remainingCapacity() > 0)) { // For ArrayBlockingQueue
					(blockingQueue.size() < queueCapacity)) { // For LinkedBlockingQueue 
					ExecutionTask executionTaskItem = hazelcastTaskQueue.take();
					logger.info ("Consumed Execution Task from Hazelcast Task Queue");
					if ( (HazelcastInstanceUtils.getStopProcessingSignal()).equals(executionTaskItem.getTaskType()) ) {
						logger.info ("Detected " + HazelcastInstanceUtils.getStopProcessingSignal());
						hzClient.getQueue(HazelcastInstanceUtils.getTaskQueueName()).put(new ExecutionTask(HazelcastInstanceUtils.getStopProcessingSignal()));
						break;
					}				
					executorPool.execute(new RunnableWorkerThread(executionTaskItem, calcResultsMap));
					totalExecutions++; 
				}
			}
			logger.info ("Hazelcast consumer Finished");

			// Shut down the pool 
			logger.info ("Shutting down executor pool..."); 
			executorPool.shutdown(); 
			logger.info (totalExecutions + " tasks. No additional tasks will be accepted"); 

			// Shut down the monitor thread 
			while (!executorPool.isTerminated()) { 
				logger.debug ("Waiting for all the Executor to terminate"); 
				Thread.sleep(monitorSleep*1000); 
			} 

			logger.info ("Executor terminated"); 
			long stopTime = System.currentTimeMillis();

			logger.info ("Shutting down monitor thread..."); 
			monitor.shutdown(); 
			logger.info ("Shutting down monitor thread... done"); 

			// Update WorkerDetails status to inactive
			workerDetail.setCalculationResults(calcResultsMap);
			workerDetail.setActiveStatus(false);
			workerDetail.setStopTime(stopTime);
			workerDetail.setTotalElapsedTime((stopTime - startTime));
			workerDetail.setTotalExecutions(totalExecutions);
			workerDetail.setAvgExecutionTime(executorPool.getAvgExecutionTime());
			workerDetail.setTotalHistoricalDataLoaded(executorPool.getTotalHistDataLoaded());
			workerDetail.setTotalCalculations(executorPool.getTotalCalculations());
			workerDetail.setTotalBasicResults(executorPool.getTotalBasicResults());
			workerDetail.setTotalSpreadResults(executorPool.getTotalSpreadResults());
			hzClient.getMap(HazelcastInstanceUtils.getMonitorMapName()).put(workerDetail.getUuid(),workerDetail);
			
			// Shutdown Hazelcast cluster node instance		
			logger.info ("Shutting down hazelcast client...");
			hzClient.getLifecycleService().shutdown();
			
			// Print statistics
			printParameters ("Finished");
			logger.info ("Results:"); 
			logger.info ("**************************************************"); 
			logger.info ("  - Start time  : " + new Timestamp(startTime)); 
			logger.info ("  - Stop time   : " + new Timestamp(stopTime)); 

			long millis = stopTime - startTime;
			long days = TimeUnit.MILLISECONDS.toDays(millis);
			millis -= TimeUnit.DAYS.toMillis(days); 
			long hours = TimeUnit.MILLISECONDS.toHours(millis);
			millis -= TimeUnit.HOURS.toMillis(hours);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
			millis -= TimeUnit.MINUTES.toMillis(minutes); 
			long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

			logger.info ("  - Elapsed time: " + (stopTime - startTime) + " ms - (" + hours + " hrs " + minutes + " min " + seconds + " secs)"); 
			logger.info ("**************************************************"); 			
		} else {
			logger.error("Unable to create Hazelcast client instance. Finishing Worker");
		}
		// Exit application
		System.exit(0);
	}
	
	private static void loadWorkerProperties () {
		// Load properties from file
		ApplicationProperties.loadApplicationProperties ();
		
		poolCoreSize = ApplicationProperties.getIntProperty("workerpool.coreSize");
		poolMaxSize = ApplicationProperties.getIntProperty("workerpool.maxSize");
		queueCapacity = ApplicationProperties.getIntProperty("workerpool.queueCapacity");
		timeoutSecs = ApplicationProperties.getIntProperty("workerpool.timeoutSecs");
		retrySleepTime = ApplicationProperties.getIntProperty("workerpool.retrySleepTime");
		retryMaxAttempts = ApplicationProperties.getIntProperty("workerpool.retryMaxAttempts");
		initialSleep = ApplicationProperties.getIntProperty("workerpool.initialSleep");
		monitorSleep = ApplicationProperties.getIntProperty("workerpool.monitorSleep");
	}
	
	// Creates Hazelcast client instance
	// Retries <retryMaxAttempts> times after <retrySleepTime> delay
	private static boolean createHazelcastClientInstance () throws Exception{
		
		boolean result = false;
		
		for (int i=0; i<retryMaxAttempts; i++) {
			try {
				hzClient = HazelcastClient.newHazelcastClient();
				result = true;
			} catch (Exception e) {
				logger.error("Exception. Unable to create Hazelcast client instance. Attempt [" + i + "/" + retryMaxAttempts + "]. Checking again after " + retrySleepTime + " secs");
				logger.error("Exception details: " + e.getClass() + " - " + e.getMessage());
				Thread.sleep(retrySleepTime*1000);
			}
		}
		return result;
	}
	
	// Print worker pool execution parameters 
	private static void printParameters (final String title) {
		logger.info ("");
		logger.info ("**************************************************"); 
		logger.info (title + " WorkerPool with the following parameters:"); 
		logger.info ("**************************************************"); 
		logger.info ("  - pool core size       : " + poolCoreSize); 
		logger.info ("  - pool max size        : " + poolMaxSize); 
		logger.info ("  - queue capacity       : " + queueCapacity); 
		logger.info ("  - timeout (secs)       : " + timeoutSecs); 
		logger.info ("  - number of tasks      : " + totalExecutions); 
		logger.info ("  - retry sleep (secs)   : " + retrySleepTime); 
		logger.info ("  - retry max attempts   : " + retryMaxAttempts);
		logger.info ("  - initial sleep (secs) : " + initialSleep); 
		logger.info ("  - monitor sleep (secs) : " + monitorSleep); 
		logger.info ("**************************************************");
	}
} 