package datamodel;
import java.io.Serializable;
import java.util.Map;

public class CalcResult implements Serializable {

	private static final long serialVersionUID = 1L;
	private String currencyPair;
	private Map<String,Integer> basicResults;
	private Map<String,Integer> spreadResults;
	private Map<String,Integer> c1212Results;
	
	public CalcResult(final String currencyPair, final Map<String,Integer> basicResults, final Map<String,Integer> spreadResults, final Map<String,Integer> c1212Results) {

		this.currencyPair = currencyPair;
		this.basicResults = basicResults;
		this.spreadResults = spreadResults;
		this.c1212Results = c1212Results;
	}
	
	public final String getCurrencyPair() { return currencyPair; }
	public final Map<String,Integer> getBasicResults() { return basicResults; }
	public final Map<String,Integer> getSpreadResults() { return spreadResults; }
	public final Map<String,Integer> get1212Results() { return c1212Results; }
} 