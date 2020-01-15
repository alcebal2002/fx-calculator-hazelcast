package datamodel;
import java.io.Serializable;
import java.util.Properties;

public class ExecutionTask implements Serializable {
	
	private static final long serialVersionUID = 1L;
	// Task parameters
	private int taskId;
	private String taskType = null;
	private String currentCurrency = null;
	private String startDate = null;
	private String endDate = null;
	private Properties applicationParameters;
	
	private CalculationResult calculationResult = null;

	public ExecutionTask(String stopSignal) {
		this.taskType = stopSignal;
	}

	public ExecutionTask(final int taskId, final String taskType, final String currentCurrency, 
						 final String startDate, final String endDate, final Properties applicationParameters) {
		this.taskId = taskId;
		this.taskType = taskType;
		this.currentCurrency = currentCurrency;
		this.startDate = startDate;
		this.endDate = endDate;
		this.applicationParameters = applicationParameters;
	}

	public final int getTaskId() {
		return this.taskId;
	}

	public final String getTaskType() {
		return this.taskType;
	}

	public final String getCurrentCurrency() {
		return this.currentCurrency;
	}

	public final String getStartDate() {
		return this.startDate;
	}

	public final String getEndDate() {
		return this.endDate;
	}

	public final Properties getApplicationParameters () {
		return applicationParameters;
	}
	
	public final CalculationResult getCalculationResult () {
		return calculationResult;
	}

	public final void setCalculationResult (CalculationResult calculationResult) {
		this.calculationResult = calculationResult;
	}
} 