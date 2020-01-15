package utils;

public class Constants {
	// Application properties file
	public static final String APPLICATION_PROPERTIES = "application.properties";
	
	// Hazelcast Properties
	// Queue instance names
	public static final String HZ_TASK_QUEUE_NAME = "taskQueue";

	// List instance names
	
	// Map instance names
	public static final String HZ_RESULTS_MAP_NAME = "resultsMap";
	public static final String HZ_WORKERS_MAP_NAME = "workersMap";
	public static final String HZ_STATUS_MAP_NAME = "statusMap";
	public static final String HZ_STATUS_ENTRY_KEY = "status";
	
	public static final String HZ_STATUS_STARTING_APPLICATION = "Starting Application";
	public static final String HZ_STATUS_PUBLISHING_TASKS = "Publishing Tasks";
	public static final String HZ_STATUS_WAITING_WORKERS = "Waiting for Workers to finish processing";
	public static final String HZ_STATUS_SHUTTING_DOWN = "Shutting down";
	public static final String HZ_STATUS_PROCESS_COMPLETED = "Process Completed";
	public static final String HZ_STATUS_APPLICATION_FINSIHED = "Application Finished";
	
	// Stop process signal
	public static final String HZ_STOP_PROCESSING_SIGNAL = "STOP_PROCESSING_SIGNAL";
	
	// Spark messages
	public static final String SPARK_WELCOME_MESSAGE = "Welcome to Spark !";
	public static final String SPARK_BYE_MESSAGE = "Bye Spark !";
	
	// Chart.js
	public static final String[] CHART_DATA_COLORS = {"red","orange","yellow","green","blue","purple","grey"};
	
	// Wokerpool properties
	public static final String WP_CORE_SIZE = "workerpool.coreSize";
	public static final String WP_MAX_SIZE = "workerpool.maxSize";
	public static final String WP_QUEUE_CAPACITY = "workerpool.queueCapacity";
	public static final String WP_TIMEOUT_SECS = "workerpool.timeoutSecs";
	public static final String WP_INITIAL_SLEEP = "workerpool.initialSleep";
	public static final String WP_RETRY_SLEEP_TIME = "workerpool.retrySleepTime";
	public static final String WP_RETRY_MAX_ATTEMPTS = "workerpool.retryMaxAttempts";
	public static final String WP_MONITOR_SLEEP = "workerpool.monitorSleep";
	public static final String WP_REFRESH_AFTER = "workerpool.refreshAfter";
	
}
