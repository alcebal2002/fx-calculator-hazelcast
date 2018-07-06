package datamodel;
import java.io.Serializable;
import java.util.Map;

public class CalcResult implements Serializable {

	private static final long serialVersionUID = 1L;
	private String currencyPair;
	private float increase;
	private float decrease;
	private int maxLevels;
	private int maxFirstIterations;
	private float spread;
	private long histDataStartTime;	
	private long histDataStopTime;
	private long totalHistDataLoaded;
	private long calculationStartTime;	
	private long calculationStopTime;
	private long totalCalculations;
	private Map<String,Integer> basicResults;
	private Map<String,Integer> spreadResults;
	private Map<String,Integer> c1212Results;
	
	public CalcResult(final String currencyPair, final float increase, final float decrease, final int maxLevels, final int maxFirstIterations, final float spread, final long histDataStartTime, final long histDataStopTime, final long totalHistDataLoaded, final long calculationStartTime, final long calculationStopTime, final long totalCalculations, final Map<String,Integer> basicResults, final Map<String,Integer> spreadResults, final Map<String,Integer> c1212Results) {
		this.currencyPair = currencyPair;
		this.increase = increase;
		this.decrease = decrease;
		this.maxLevels = maxLevels;
		this.maxFirstIterations = maxFirstIterations;
		this.spread = spread;
		this.histDataStartTime = histDataStartTime;
		this.histDataStopTime = histDataStopTime;
		this.totalHistDataLoaded = totalHistDataLoaded;
		this.calculationStartTime = calculationStartTime;
		this.calculationStopTime = calculationStopTime;
		this.totalCalculations = totalCalculations;
		this.basicResults = basicResults;
		this.spreadResults = spreadResults;
		this.c1212Results = c1212Results;
	}
	
	public final String getCurrencyPair() { return currencyPair; }
	public final float getIncrease() { return increase; }
	public final float getDecrease() { return decrease; }
	public final int getmaxLevels() { return maxLevels; }
	public final int getmaxFirstIterations() { return maxFirstIterations; }
	public final float getSpread() { return spread; }
	public final long getHistDataStartTime() { return histDataStartTime; }
	public final long getHistDataStopTime() { return histDataStopTime; }
	public final long getTotalHistDataLoaded() { return totalHistDataLoaded; }
	public final long getCalculationStartTime() { return calculationStartTime; }
	public final long getCalculationStopTime() { return calculationStopTime; }
	public final long getTotalCalculations() { return totalCalculations; }
	public final Map<String,Integer> getBasicResults() { return basicResults; }
	public final Map<String,Integer> getSpreadResults() { return spreadResults; }
	public final Map<String,Integer> get1212Results() { return c1212Results; }
} 