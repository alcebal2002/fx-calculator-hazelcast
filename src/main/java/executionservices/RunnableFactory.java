package executionservices;

import datamodel.ExecutionTask;
import runnables.RunnableThread1212;
import runnables.RunnableThread1234;
import runnables.RunnableThreadBasic;
import runnables.RunnableThreadSpread;

public class RunnableFactory {		
	//use getRunnable method to get object of type Runnable 
	public Runnable getRunnable(String calculationMethodology, ExecutionTask executionTask){
		if(calculationMethodology == null){
		return null;
		}
		
		if(calculationMethodology.equalsIgnoreCase("BASIC")){
			return new RunnableThreadBasic (executionTask);	     
		} else if(calculationMethodology.equalsIgnoreCase("SPREAD")){
			return new RunnableThreadSpread (executionTask);	  
		} else if(calculationMethodology.equalsIgnoreCase("1212")){
			return new RunnableThread1212 (executionTask);
		} else if(calculationMethodology.equalsIgnoreCase("1234")){
			return new RunnableThread1234 (executionTask);
		}
		return null;
	}
}
