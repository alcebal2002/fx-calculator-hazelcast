package datamodel;
import java.io.Serializable;
import java.sql.Timestamp;

public class WorkerDetail implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String uuid;
	private String inetAddres;
	private String inetPort;
	private boolean activeStatus = true;
	private long refreshTime = System.currentTimeMillis();

	// Start up parameters
	private int poolCoreSize;
	private int poolMaxSize; 
	private int queueCapacity; 
	private int timeoutSecs; 
	private int retrySleepTime; 
	private int retryMaxAttempts; 
	private int initialSleep; 
	private int monitorSleep;

	private long startTime = 0L; 
	private long stopTime = 0L;
	private long totalElapsedTime = 0L;
	
	/**
	 * @param uuid
	 * @param inetAddres
	 * @param inetPort
	 * @param poolCoreSize
	 * @param poolMaxSize
	 * @param queueCapacity
	 * @param timeoutSecs
	 * @param retrySleepTime
	 * @param retryMaxAttempts
	 * @param initialSleep
	 * @param monitorSleep
	 * @param startTime
	 * @param stopTime
	 */
	public WorkerDetail(String uuid, String inetAddres, String inetPort, int poolCoreSize, int poolMaxSize,
			int queueCapacity, int timeoutSecs, int retrySleepTime, int retryMaxAttempts,
			int initialSleep, int monitorSleep, long startTime) {
		this.uuid = uuid;
		this.inetAddres = inetAddres;
		this.inetPort = inetPort;
		
		this.poolCoreSize = poolCoreSize;
		this.poolMaxSize = poolMaxSize;
		this.queueCapacity = queueCapacity;
		this.timeoutSecs = timeoutSecs;
		this.retrySleepTime = retrySleepTime;
		this.retryMaxAttempts = retryMaxAttempts;
		this.initialSleep = initialSleep;
		this.monitorSleep = monitorSleep;
		this.startTime = startTime;
	}

	public final String getUuid() {	return this.uuid; }
	public final void setNodeId(String uuid) { this.uuid = uuid; }
	public final boolean getActiveStatus() { return activeStatus; }
	public final void setActiveStatus(boolean status) {	this.activeStatus = status;	}
	public final String getActiveStatusString() { return activeStatus?"Active":"Inactive"; }
	public final long getRefreshTime() {	return refreshTime; }
	public final void setRefreshTime (long refreshTime) { this.refreshTime = refreshTime; }
	public final String getInetAddres() { return inetAddres; }
	public final String getInetPort() {	return inetPort; }
	public final int getPoolCoreSize() { return poolCoreSize; }
	public final int getPoolMaxSize() {	return poolMaxSize;	}
	public final int getQueueCapacity() { return queueCapacity;	}
	public final int getTimeoutSecs() {	return timeoutSecs;	}
	public final int getRetrySleepTime() { return retrySleepTime; }
	public final int getRetryMaxAttempts() { return retryMaxAttempts; }
	public final int getInitialSleep() { return initialSleep; }
	public final int getMonitorSleep() { return monitorSleep; }
	public final long getStartTime() { return startTime; }
	public final String getStartTimeString() { return ((this.getStartTime()>0L)?(new Timestamp(this.getStartTime()).toString()):" - ");	}
	public final long getStopTime() { return stopTime; }
	public final String getStopTimeString() { return ((this.getStopTime()>0L)?(new Timestamp(this.getStopTime()).toString()):" - "); }
	public final void setStopTime(long stopTime) { this.stopTime = stopTime; }	
	public final long getTotalElapsedTime() { return totalElapsedTime; }
	public final String getTotalElapsedTimeString() { return ((this.getTotalElapsedTime()>0L)?(new Timestamp(this.getTotalElapsedTime()).toString()):" - "); }
	public final void setTotalElapsedTime(long totalElapsedTime) { this.totalElapsedTime = totalElapsedTime; }	

	public final String getCsvFormat() {
		return  this.getUuid() + ";" +
				this.getInetAddres() + ";" +
				this.getInetPort() + ";" +
				this.getStartTimeString() + ";" +
				this.getStopTimeString();
	}
} 