import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

import datamodel.ExecutionTask;
import datamodel.WorkerDetail;
import executionservices.RejectedExecutionHandlerImpl;
import executionservices.SystemLinkedBlockingQueue;
import executionservices.SystemMonitorThread;
import executionservices.SystemThreadPoolExecutor;
import runnables.RunnableCalculationFactory;
import utils.ApplicationProperties;
import utils.Constants;
import utils.GeneralUtils;
import utils.HazelcastInstanceUtils;

public class Worker {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(Worker.class);
	
	// Hazelcast client
	private static HazelcastInstance hzClient;
	
	private static String nodeId;
	private static String localEndPointAddress;
	private static String localEndPointPort;
	
	private static long totalExecutions = 0;
	
	public static void main(String args[]) throws Exception {
		
		logger.info("WorkerPool started");
		
		// Load properties from file
		ApplicationProperties.loadApplicationProperties ();
		
		logger.info ("Waiting " + ApplicationProperties.getIntProperty(Constants.WP_INITIAL_SLEEP) + " secs to start..."); 
		Thread.sleep(ApplicationProperties.getIntProperty(Constants.WP_INITIAL_SLEEP)*1000); 

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
			SystemThreadPoolExecutor executorPool = new SystemThreadPoolExecutor(ApplicationProperties.getIntProperty(Constants.WP_CORE_SIZE), 
																				 ApplicationProperties.getIntProperty(Constants.WP_MAX_SIZE), 
																				 ApplicationProperties.getIntProperty(Constants.WP_TIMEOUT_SECS),
																				 TimeUnit.SECONDS, blockingQueue, threadFactory, rejectionHandler,hzClient); 
			
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
					ApplicationProperties.getIntProperty(Constants.WP_CORE_SIZE),
					ApplicationProperties.getIntProperty(Constants.WP_MAX_SIZE),
					ApplicationProperties.getIntProperty(Constants.WP_QUEUE_CAPACITY),
					ApplicationProperties.getIntProperty(Constants.WP_TIMEOUT_SECS),
					ApplicationProperties.getIntProperty(Constants.WP_RETRY_SLEEP_TIME),
					ApplicationProperties.getIntProperty(Constants.WP_RETRY_MAX_ATTEMPTS),
					ApplicationProperties.getIntProperty(Constants.WP_INITIAL_SLEEP),
					ApplicationProperties.getIntProperty(Constants.WP_MONITOR_SLEEP),
					startTime);

			// Start the monitoring thread 
			SystemMonitorThread monitor = new SystemMonitorThread(executorPool, ApplicationProperties.getIntProperty(Constants.WP_MONITOR_SLEEP), nodeId); 
			Thread monitorThread = new Thread(monitor); 
			monitorThread.start(); 

			hzClient.getMap(HazelcastInstanceUtils.getWorkersMapName()).put(workerDetail.getUuid(),workerDetail);
			
			// Listen to Hazelcast tasks queue and submit work to the thread pool for each task 
			IQueue<ExecutionTask> hazelcastTaskQueue = hzClient.getQueue( HazelcastInstanceUtils.getTaskQueueName() );		

			long refreshTime = System.currentTimeMillis();
			logger.info ("Refreshing Hazelcast WorkerDetail status after " + ApplicationProperties.getIntProperty(Constants.WP_REFRESH_AFTER) + " secs");

			RunnableCalculationFactory runnableFactory = new RunnableCalculationFactory();
			
			while ( true ) {
				/*
				 * Option to avoid getting additional tasks from Hazelcast distributed queue if there is no processing capacity available in the ThreadPool 
				 */
				if ((executorPool.getActiveCount() < executorPool.getMaximumPoolSize()) ||
//					(blockingQueue.remainingCapacity() > 0)) { // For ArrayBlockingQueue
					(blockingQueue.size() < ApplicationProperties.getIntProperty(Constants.WP_QUEUE_CAPACITY))) { // For LinkedBlockingQueue 
					ExecutionTask executionTaskItem = hazelcastTaskQueue.take();
					logger.info ("Consumed Execution Task " + executionTaskItem.getTaskId() + " (" + executionTaskItem.getCurrentCurrency() + " - " + executionTaskItem.getTaskType() + ") from Hazelcast Task Queue");
					if ( (HazelcastInstanceUtils.getStopProcessingSignal()).equals(executionTaskItem.getTaskType()) ) {
						logger.info ("Detected " + HazelcastInstanceUtils.getStopProcessingSignal());
						hzClient.getQueue(HazelcastInstanceUtils.getTaskQueueName()).put(new ExecutionTask(HazelcastInstanceUtils.getStopProcessingSignal()));
						break;
					}
					
					// Determines which Runnable has to execute the task based on the taskType (ie. basic, spread...)
					try {
						executorPool.execute(runnableFactory.getRunnable(executionTaskItem));
					} catch (Exception ex) {
						logger.error("Unable to find Runnable for Execution Task " + executionTaskItem.getTaskId() + " (" + executionTaskItem.getCurrentCurrency() + " - " + executionTaskItem.getTaskType() + ")");
					} finally {
						totalExecutions++;
						workerDetail.setTotalExecutions(totalExecutions);
					}
				}
				
				if ((System.currentTimeMillis()) - refreshTime > (ApplicationProperties.getIntProperty(Constants.WP_REFRESH_AFTER)*1000)) {
					refreshTime = System.currentTimeMillis();
					workerDetail.setRefreshTime(refreshTime);
					hzClient.getMap(HazelcastInstanceUtils.getWorkersMapName()).put(workerDetail.getUuid(),workerDetail);
					if (logger.isDebugEnabled())
						logger.debug ("Updated Hazelcast WorkerDetail refreshTime");
				}
			}
			logger.info ("Hazelcast consumer Finished");

			// Shut down the pool 
			logger.info ("Shutting down executor pool..."); 
			executorPool.shutdown(); 
			logger.info (totalExecutions + " tasks. No additional tasks will be accepted"); 

			// Shut down the monitor thread 
			while (!executorPool.isTerminated()) { 
				if (logger.isDebugEnabled())
					logger.debug ("Waiting for all the Executor to terminate"); 
				Thread.sleep(ApplicationProperties.getIntProperty(Constants.WP_MONITOR_SLEEP)*1000); 
			} 

			logger.info ("Executor terminated"); 
			long stopTime = System.currentTimeMillis();

			logger.info ("Shutting down monitor thread..."); 
			monitor.shutdown(); 
			logger.info ("Shutting down monitor thread... done"); 

			// Update WorkerDetails status to inactive
			workerDetail.setActiveStatus(false);
			workerDetail.setStopTime(stopTime);
			workerDetail.setTotalElapsedTime((stopTime - startTime));
			hzClient.getMap(HazelcastInstanceUtils.getWorkersMapName()).put(workerDetail.getUuid(),workerDetail);
			
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
	
	// Creates Hazelcast client instance
	// Retries <retryMaxAttempts> times after <retrySleepTime> delay
	private static boolean createHazelcastClientInstance () throws Exception{
		
		boolean result = false;
		
		for (int i=0; (i<ApplicationProperties.getIntProperty(Constants.WP_RETRY_MAX_ATTEMPTS)) && (!result); i++) {
			try {
				hzClient = HazelcastClient.newHazelcastClient();
				result = true;
			} catch (Exception e) {
				logger.error("Exception. Unable to create Hazelcast client instance. Attempt [" + i + "/" + ApplicationProperties.getIntProperty(Constants.WP_RETRY_MAX_ATTEMPTS) + "]. Checking again after " + ApplicationProperties.getIntProperty(Constants.WP_RETRY_SLEEP_TIME) + " secs");
				logger.error("Exception details: " + e.getClass() + " - " + e.getMessage());
				Thread.sleep(ApplicationProperties.getIntProperty(Constants.WP_RETRY_SLEEP_TIME)*1000);
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
		logger.info ("  - pool core size       : " + ApplicationProperties.getIntProperty(Constants.WP_CORE_SIZE)); 
		logger.info ("  - pool max size        : " + ApplicationProperties.getIntProperty(Constants.WP_MAX_SIZE)); 
		logger.info ("  - queue capacity       : " + ApplicationProperties.getIntProperty(Constants.WP_QUEUE_CAPACITY)); 
		logger.info ("  - timeout (secs)       : " + ApplicationProperties.getIntProperty(Constants.WP_TIMEOUT_SECS)); 
		logger.info ("  - number of tasks      : " + totalExecutions); 
		logger.info ("  - retry sleep (secs)   : " + ApplicationProperties.getIntProperty(Constants.WP_RETRY_SLEEP_TIME)); 
		logger.info ("  - retry max attempts   : " + ApplicationProperties.getIntProperty(Constants.WP_RETRY_MAX_ATTEMPTS));
		logger.info ("  - initial sleep (secs) : " + ApplicationProperties.getIntProperty(Constants.WP_INITIAL_SLEEP)); 
		logger.info ("  - monitor sleep (secs) : " + ApplicationProperties.getIntProperty(Constants.WP_MONITOR_SLEEP)); 
		logger.info ("**************************************************");
	}
} 