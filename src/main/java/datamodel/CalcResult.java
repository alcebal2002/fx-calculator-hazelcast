package datamodel;
import java.io.Serializable;
import java.util.Map;

public class CalcResult implements Serializable {

	private static final long serialVersionUID = 1L;
	private String currencyPair;
	private float increase;
	private float decrease;
	private int maxLevels;
	private float spread;
	private long histDataStartTime;	
	private long histDataStopTime;
	private long totalHistDataLoaded;
	private long calculationStartTime;	
	private long calculationStopTime;
	private long totalCalculations;
	private Map<String,Integer> basicResults;
	private Map<String,Integer> spreadResults;
	
	public CalcResult(final String currencyPair, final float increase, final float decrease, final int maxLevels, final float spread, final long histDataStartTime, final long histDataStopTime, final long totalHistDataLoaded, final long calculationStartTime, final long calculationStopTime, final long totalCalculations, final Map<String,Integer> basicResults, final Map<String,Integer> spreadResults) {
		this.currencyPair = currencyPair;
		this.increase = increase;
		this.decrease = decrease;
		this.maxLevels = maxLevels;
		this.spread = spread;
		this.histDataStartTime = histDataStartTime;
		this.histDataStopTime = histDataStopTime;
		this.totalHistDataLoaded = totalHistDataLoaded;
		this.calculationStartTime = calculationStartTime;
		this.calculationStopTime = calculationStopTime;
		this.totalCalculations = totalCalculations;
		this.basicResults = basicResults;
		this.spreadResults = spreadResults;
	}
	
	public final String getCurrencyPair() { return currencyPair; }
	public final float getIncrease() { return increase; }
	public final float getDecrease() { return decrease; }
	public final int getmaxLevels() { return maxLevels; }
	public final float getSpread() { return spread; }
	public final long getHistDataStartTime() { return histDataStartTime; }
	public final long getHistDataStopTime() { return histDataStopTime; }
	public final long getTotalHistDataLoaded() { return totalHistDataLoaded; }
	public final long getCalculationStartTime() { return calculationStartTime; }
	public final long getCalculationStopTime() { return calculationStopTime; }
	public final long getTotalCalculations() { return totalCalculations; }
	public final Map<String,Integer> getBasicResults() { return basicResults; }
	public final Map<String,Integer> getSpreadResults() { return spreadResults; }
} 