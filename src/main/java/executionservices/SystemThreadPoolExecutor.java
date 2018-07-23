package executionservices;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import datamodel.CalcResult;
import runnables.RunnableThreadBasic; 
  
public class SystemThreadPoolExecutor extends ThreadPoolExecutor { 
        
	private Map<String,CalcResult> calcResultsMap = new HashMap<String,CalcResult>();
	private long totalExecutionTime = 0;
    private int totalExecutions = 0;
    private long totalCalculations = 0;
    private long totalHistDataLoaded = 0;
	private long totalBasicResults = 0;
	private long totalSpreadResults = 0;
	private long total1212Results = 0;
	private long total1234Results = 0;

    public SystemThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, 
    							BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) { 
    	super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    } 

    protected void afterExecute (Runnable r, Throwable t ) { 
            
	    try {
	    	totalExecutions++;
	    	
	    	calcResultsMap.putAll(((RunnableThreadBasic)r).getCalcResultsMap());
			totalExecutionTime += ((RunnableThreadBasic)r).getElapsedTimeMillis();
			totalHistDataLoaded += ((RunnableThreadBasic)r).getTotalHistDataLoaded();
			totalCalculations += ((RunnableThreadBasic)r).getTotalCalculations();
			totalBasicResults += ((RunnableThreadBasic)r).getTotalBasicResults();
			totalSpreadResults += ((RunnableThreadBasic)r).getTotalSpreadResults();
			total1212Results += ((RunnableThreadBasic)r).getTotal1212Results();
			total1234Results += ((RunnableThreadBasic)r).getTotal1234Results();
	    } finally { 
	    	super.afterExecute(r, t); 
	    } 
    }
    
    public Map<String,CalcResult> getCalcResultsMap () { return this.calcResultsMap; }
    public long getTotalExecutionTime () { return this.totalExecutionTime; }
    public long getTotalExecutions () { return this.totalExecutions; }
	public long getTotalHistDataLoaded () { return this.totalHistDataLoaded; }
	public long getTotalCalculations () { return this.totalCalculations; }
	public long getTotalBasicResults () { return this.totalBasicResults; }
	public long getTotalSpreadResults () { return this.totalSpreadResults; }
	public long getTotal1212Results () { return this.total1212Results; }
	public long getTotal1234Results () { return this.total1234Results; }

    public long getAvgExecutionTime () {
    	long result = 0L;
    	
    	if (this.totalExecutions > 0) {
    		result = this.totalExecutionTime / this.totalExecutions;
    	}
    	
    	return result;
    } 
} 