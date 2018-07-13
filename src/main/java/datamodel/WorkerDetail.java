package datamodel;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

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
	private long totalExecutions = 0;
	private long totalHistoricalDataLoaded = 0;
	private long totalCalculations = 0;
	private long totalBasicResults = 0;
	private long totalSpreadResults = 0;
	private long total1212Results = 0;
	private long total1234Results = 0;
	private long avgExecutionTime = 0L;
	
	private Map<String,CalcResult> calcResultsMap;
	
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
	 * @param elapsedArray
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
	public final String getInetAddres() { return inetAddres; }
	public final String getInetPort() {	return inetPort; }
	public final long getRefreshTime() {	return refreshTime; }
	public final void setRefreshTime (long refreshTime) { this.refreshTime = refreshTime; }
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
	public final void setStartTime(long startTime) { this.startTime = startTime; }
	public final long getStopTime() { return stopTime; }
	public final String getStopTimeString() { return ((this.getStopTime()>0L)?(new Timestamp(this.getStopTime()).toString()):" - "); }
	public final void setStopTime(long stopTime) { this.stopTime = stopTime; }	
	public final void setCalculationResults(Map<String,CalcResult> calcResults) { this.calcResultsMap = calcResults; }
	public final Map<String,CalcResult> getCalculationResults() { return calcResultsMap; }
	public final long getAvgExecutionTime() { return avgExecutionTime; }
	public final void setAvgExecutionTime(long avgExecutionTime) { this.avgExecutionTime = avgExecutionTime; }
	public final long getTotalCalculations() { return totalCalculations; }
	public final void setTotalCalculations(long totalCalculations) { this.totalCalculations = totalCalculations; }
	public final long getTotalBasicResults() { return totalBasicResults; }
	public final void setTotalBasicResults(long totalBasicResults) { this.totalBasicResults = totalBasicResults; }
	public final long getTotalSpreadResults() { return totalSpreadResults; }
	public final void setTotalSpreadResults(long totalSpreadResults) { this.totalSpreadResults = totalSpreadResults; }
	public final long getTotal1212Results() { return total1212Results; }
	public final void setTotal1212Results(long total1212Results) { this.total1212Results = total1212Results; }
	public final long getTotal1234Results() { return total1234Results; }
	public final void setTotal1234Results(long total1234Results) { this.total1234Results = total1234Results; }
	public final long getTotalHistoricalDataLoaded() { return totalHistoricalDataLoaded; }
	public final void setTotalHistoricalDataLoaded(long totalHistoricalDataLoaded) { this.totalHistoricalDataLoaded = totalHistoricalDataLoaded; }
	public final long getTotalExecutions() { return totalExecutions; }
	
	public final String getTotalExecutionsWithoutComma() {
		String regex = "(?<=\\d),(?=\\d)";
		return (""+totalExecutions).replaceAll(regex, "");
	}

	public final void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }
	public final long getTotalElapsedTime() { return totalElapsedTime; }
	public final void setTotalElapsedTime(long totalElapsedTime) { this.totalElapsedTime = totalElapsedTime; }

	public final String getCsvFormat() {
		return  this.getUuid() + ";" +
				this.getInetAddres() + ";" +
				this.getInetPort() + ";" +
				this.getStartTimeString() + ";" +
				this.getStopTimeString() + ";" +
				this.getTotalElapsedTime() + ";" +
				this.getTotalExecutions();
				//((this.getStopTime()>0L)?(new Timestamp(this.getStopTime())):" - ") + ";"; 
	}
} 