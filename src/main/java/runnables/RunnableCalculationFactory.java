package runnables;

import datamodel.ExecutionTask;

public class RunnableCalculationFactory {		
	//use getRunnable method to get object of type Runnable 
	public Runnable getRunnable(ExecutionTask executionTask){
		
		Runnable result = null;
		
		if(executionTask.getCalculationMethodology() == null){
			result = null;
		} else if((executionTask.getCalculationMethodology()).equalsIgnoreCase("BASIC")){
			result = new RunnableThreadBasic (executionTask);	     
		} else if((executionTask.getCalculationMethodology()).equalsIgnoreCase("SPREAD")){
			result = new RunnableThreadSpread (executionTask);	  
		} else if((executionTask.getCalculationMethodology()).equalsIgnoreCase("1212")){
			result = new RunnableThread1212 (executionTask);
		} else if((executionTask.getCalculationMethodology()).equalsIgnoreCase("1234")){
			result = new RunnableThread1234 (executionTask);
		}
		return result;
	}
}