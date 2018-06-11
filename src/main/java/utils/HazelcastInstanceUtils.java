package utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;

import datamodel.ExecutionTask;

public class HazelcastInstanceUtils {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(HazelcastInstanceUtils.class);

	private static HazelcastInstance hazelcastInstance;

	public static synchronized HazelcastInstance getInstance () {
		if (hazelcastInstance == null) {
			hazelcastInstance = Hazelcast.newHazelcastInstance(); 
		}
		
		return hazelcastInstance;
	}
	
	public static String getTaskQueueName () throws Exception {
		return Constants.HZ_TASK_QUEUE_NAME;
	}

	public static String getMonitorMapName () throws Exception {
		return Constants.HZ_MONITOR_MAP_NAME;
	}

	public static String getStatusMapName () throws Exception {
		return Constants.HZ_STATUS_MAP_NAME;
	}

	public static String getStopProcessingSignal () throws Exception {
		return Constants.HZ_STOP_PROCESSING_SIGNAL;
	}

	public static void putStopSignalIntoQueue (final String queueName) throws Exception {
		logger.info ("Sending " + getStopProcessingSignal() + " to " + queueName);
		putIntoQueue(queueName,(new ExecutionTask(getStopProcessingSignal())));
	}

	public static void putIntoQueue (final String queueName, final Object value) {
		try {
			getInstance().getQueue(queueName).put(value);
		} catch (InterruptedException e) {
			logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
		}
	}

	public static IQueue<ExecutionTask> getExecutionTaskQueue () throws Exception {
		return getInstance().getQueue(getTaskQueueName());
	}
	
	public static IList<Object> getList (final String listName) {
		return getInstance().getList(listName);
	}

	public static IMap<String,Object> getMap  (final String mapName) {
		return getInstance().getMap(mapName);
	}

	public static Object getFromMap  (final String mapName, final String key) {
		return getInstance().getMap(mapName).get(key);
	}
	
	public static void putIntoMap (final String mapName, final String key, final Object value) {
		getInstance().getMap(mapName).put(key,value);
	}
	
	public static void removeFromMap (final String mapName, final String key) {
		getInstance().getMap(mapName).remove(key);
	}

	public static void putIntoList (final String listName, final Object value) {
		getInstance().getList(listName).add(value);
	}

	public static void setStatus (final String status) throws Exception {
		getInstance().getMap(getStatusMapName()).put(Constants.HZ_STATUS_ENTRY_KEY,status);
	}
	
	public static String getNodeId () {
    	String result = "unknown";
        try {
            result = getInstance().getCluster().getLocalMember().getUuid();
        } catch (Exception ex) {}
        return result;
    }

    public static String getInetAddress () {
    	String result = "unknown";
        try {
            result = getInstance().getCluster().getLocalMember().getSocketAddress().getHostName();
        } catch (Exception ex) {}
        return result;
    }

    public static int getInetPort () {
    	int result = 0;
        try {
            result = getInstance().getCluster().getLocalMember().getSocketAddress().getPort();
        } catch (Exception ex) {}
        return result;
    }
    
    public static void shutdown() {
    	getInstance().getLifecycleService().shutdown();
    }
}
