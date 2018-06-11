package utils;

public class Constants {
	// Application properties file
	public static final String APPLICATION_PROPERTIES = "application.properties";
	
	// Hazelcast Properties
	// Queue instance names
	
	public static final String HZ_TASK_QUEUE_NAME = "taskQueue";

	// List instance names
	
	// Map instance names
	public static final String HZ_MONITOR_MAP_NAME = "monitorMap";
	public static final String HZ_STATUS_MAP_NAME = "statusMap";
	public static final String HZ_STATUS_ENTRY_KEY = "status";
	
	public static final String HZ_STATUS_STARTING_APPLICATION = "Starting Application";
	public static final String HZ_STATUS_PUBLISHING_TASKS = "Publishing Tasks";
	public static final String HZ_STATUS_WAITING_WORKERS = "Waiting for Workers to finish processing";
	public static final String HZ_STATUS_SHUTTING_DOWN = "Shutting down";
	public static final String HZ_STATUS_PROCESS_COMPLETED = "Process Completed";
	
	// Stop process signal
	public static final String HZ_STOP_PROCESSING_SIGNAL = "STOP_PROCESSING_SIGNAL";
}
