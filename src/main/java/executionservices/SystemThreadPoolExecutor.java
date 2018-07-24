package executionservices;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import datamodel.ExecutionTask;
import runnables.RunnableThreadBasic; 
  
public class SystemThreadPoolExecutor extends ThreadPoolExecutor { 

	private ArrayList<ExecutionTask> resultsList = new ArrayList<ExecutionTask>();
	private long totalExecutionTime = 0;
    private int totalExecutions = 0;
    private long totalCalculations = 0;
    private long totalHistDataLoaded = 0;

    public SystemThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, 
    							BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) { 
    	super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    } 

    protected void afterExecute (Runnable r, Throwable t ) { 
            
	    try {
	    	totalExecutions++;
	    	
	    	resultsList.add(((RunnableThreadBasic)r).getExecutionTask());
			totalExecutionTime += ((RunnableThreadBasic)r).getElapsedTimeMillis();
			totalHistDataLoaded += ((RunnableThreadBasic)r).getTotalHistDataLoaded();
			totalCalculations += ((RunnableThreadBasic)r).getTotalCalculations();

	    } finally { 
	    	super.afterExecute(r, t); 
	    } 
    }
    
    public ArrayList<ExecutionTask> getResultsList () { return this.resultsList; }
    public long getTotalExecutionTime () { return this.totalExecutionTime; }
    public long getTotalExecutions () { return this.totalExecutions; }
	public long getTotalHistDataLoaded () { return this.totalHistDataLoaded; }
	public long getTotalCalculations () { return this.totalCalculations; }

    public long getAvgExecutionTime () {
    	long result = 0L;
    	
    	if (this.totalExecutions > 0) {
    		result = this.totalExecutionTime / this.totalExecutions;
    	}
    	
    	return result;
    } 
} 