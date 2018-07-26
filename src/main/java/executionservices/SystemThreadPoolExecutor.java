package executionservices;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;

import runnables.RunnableCalculation;
import utils.HazelcastInstanceUtils; 
  
public class SystemThreadPoolExecutor extends ThreadPoolExecutor { 

	// Logger
	private static Logger logger = LoggerFactory.getLogger(SystemThreadPoolExecutor.class);

	private HazelcastInstance hzClient;
	private long totalExecutionTime = 0;
    private int totalExecutions = 0;
    private long totalCalculations = 0;
    private long totalHistDataLoaded = 0;

    public SystemThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, 
    							BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler, HazelcastInstance hzClient) {
    	super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    	this.hzClient = hzClient;
    } 

    protected void afterExecute (Runnable r, Throwable t ) { 
            
	    try {
	    	totalExecutions++;
	    	
			totalExecutionTime += ((RunnableCalculation) r).getExecutionTask().getCalculationResult().getElapsedTime();
			totalHistDataLoaded += ((RunnableCalculation) r).getExecutionTask().getCalculationResult().getTotalHistoricalDataLoaded();
			totalCalculations += ((RunnableCalculation) r).getExecutionTask().getCalculationResult().getTotalCalculations();

			// PUT the ExecutionTask results into Hazelcast
			hzClient.getMap(HazelcastInstanceUtils.getResultsMapName()).put(((RunnableCalculation)r).getExecutionTask().getTaskId(),((RunnableCalculation)r).getExecutionTask());
			
	    } catch (Exception e) {
	    	logger.error("Exception: " + e.getClass() + " - " + e.getMessage());
	    } finally { 
	    	super.afterExecute(r, t); 
	    } 
    }
    
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