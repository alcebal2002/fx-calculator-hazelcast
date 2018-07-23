package datamodel;
import java.io.Serializable;
import java.util.Map;

public class CalculationResult implements Serializable {

	private static final long serialVersionUID = 1L;
	private long startTime = 0L; 
	private long stopTime = 0L;
	private long totalElapsedTime = 0L; 
	private long totalHistoricalDataLoaded = 0;
	private long totalCalculations = 0;
	private Map<String,Integer> resultsMap;
	
	public CalculationResult() {
	}
	
	public final Map<String,Integer> getResultsMap() { return resultsMap; }
	public final void setResultsMap (Map<String,Integer> resultsMap) { this.resultsMap = resultsMap; }
	public final long getStartTime() { return startTime; }
	public final void setStartTime(long startTime) { this.startTime = startTime; }
	public final long getStopTime() { return stopTime; }
	public final void setStopTime(long stopTime) { this.stopTime = stopTime; }
	public final long getTotalHistoricalDataLoaded() { return totalHistoricalDataLoaded;}
	public final void setTotalHistoricalDataLoaded(long totalHistoricalDataLoaded) { this.totalHistoricalDataLoaded = totalHistoricalDataLoaded;}
	public final long getTotalCalculations() { return totalCalculations; }
	public final void setTotalCalculations(long totalCalculations) { this.totalCalculations = totalCalculations;}
	
	public final String getTotalCalculationsWithoutComma() {
		String regex = "(?<=\\d),(?=\\d)";
		return (""+this.totalCalculations).replaceAll(regex, "");
	}

} 