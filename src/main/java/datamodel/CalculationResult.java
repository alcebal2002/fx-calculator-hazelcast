package datamodel;
import java.io.Serializable;
import java.util.Map;

public class CalculationResult implements Serializable {

	private static final long serialVersionUID = 1L;
	private Map<String,Integer> resultsMap;
	
	public CalculationResult() {
	}
	
	public final Map<String,Integer> getResultsMap() { return resultsMap; }
	public final void setResultsMap (Map<String,Integer> resultsMap) { this.resultsMap = resultsMap; } 
} 