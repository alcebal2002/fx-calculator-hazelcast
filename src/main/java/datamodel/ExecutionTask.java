package datamodel;
import java.io.Serializable;
import java.util.Properties;

public class ExecutionTask implements Serializable {
	
	private static final long serialVersionUID = 1L;
	// Task parameters
	private int taskId;
	private String taskType = null;
	private Properties taskParameters;
	
	private long creationTimestamp = 0L;
	private long elapsedExecutionTime = 0L;

	public ExecutionTask(String taskType) {
		this.taskType = taskType;
	}

	public ExecutionTask(final int taskId, final String taskType, final Properties taskParameters) {
		this.taskId = taskId;
		this.taskType = taskType;
		this.taskParameters = taskParameters;
		this.creationTimestamp = System.currentTimeMillis();
	}

	public final int getTaskId() {
		return this.taskId;
	}

	public final String getTaskType() {
		return this.taskType;
	}

	public final Properties getTaskParameters() {
		return this.taskParameters;
	}

	public final long getCreationTimestamp() {
		return creationTimestamp;
	}
	public final long getElapsedExecutionTime() {
		return elapsedExecutionTime;
	}
	public final void setElapsedExecutionTime (long elapsedTime) {
		this.elapsedExecutionTime = elapsedTime;
	}
} 