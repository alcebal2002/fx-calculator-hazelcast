package datamodel;
import java.io.Serializable;
import java.util.Properties;

public class ExecutionTask implements Serializable {
	
	private static final long serialVersionUID = 1L;
	// Task parameters
	private int taskId;
	private String calculationMethodology = null;
	private String currentCurrency = null;
	private Properties taskParameters;
	
	private WorkerDetail workerDetail = null;
	private CalculationResult calculationResult = null;

	public ExecutionTask(String stopSignal) {
		this.calculationMethodology = stopSignal;
	}

	public ExecutionTask(final int taskId, final String calculationMethodology, final String currentCurrency, final Properties taskParameters) {
		this.taskId = taskId;
		this.calculationMethodology = calculationMethodology;
		this.currentCurrency = currentCurrency;
		this.taskParameters = taskParameters;
	}

	public final int getTaskId() {
		return this.taskId;
	}

	public final String getCalculationMethodology() {
		return this.calculationMethodology;
	}

	public final String getCurrentCurrency() {
		return this.currentCurrency;
	}

	public final Properties getTaskParameters () {
		return taskParameters;
	}
	
	public final CalculationResult getCalculationResult () {
		return calculationResult;
	}

	public final void setCalculationResult (CalculationResult calculationResult) {
		this.calculationResult = calculationResult;
	}
	public final WorkerDetail getWorkerDetail () {
		return workerDetail;
	}

	public final void setWorkerDetail (WorkerDetail workerDetail) {
		this.workerDetail = workerDetail;
	}
} 