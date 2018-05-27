package utils;

public class Constants {
	// Hazelcast Properties
	// Queue instance names
	public static final String HZ_TASK_QUEUE_NAME = "hz.taskQueueName";
	public static final String HZ_RESULTS_QUEUE_NAME = "hz.resultsQueueName";

	// List instance names
	
	// Map instance names
	public static final String HZ_MONITOR_MAP_NAME = "hz.monitorMapName";
	public static final String HZ_STATUS_MAP_NAME = "hz.statusMapName";
	public static final String HZ_STATUS_ENTRY_KEY = "status";
	public static final String HZ_HISTORICAL_DATA_MAP_NAME = "hz.historicalDataMapName";
	
	public static final String HZ_STATUS_LOADING_HISTORICAL_DATA = "Loading Historical Data";
	public static final String HZ_STATUS_PUBLISHING_TASKS = "Publishing Tasks";
	public static final String HZ_STATUS_WAITING_TO_START_MONITORING = "Waiting to start Monitoring";
	public static final String HZ_STATUS_PROCESSING_TASKS = "Processing Tasks";
	public static final String HZ_STATUS_SHUTTING_DOWN = "Shutting down";
	public static final String HZ_STATUS_PROCESS_COMPLETED = "Process Completed";
	
	// Stop process signal
	public static final String HZ_STOP_PROCESSING_SIGNAL = "hz.stopProcessingSignal";
}
