package runnables;

import datamodel.ExecutionTask;

public class RunnableCalculationFactory {		
	//use getRunnable method to get object of type Runnable 
	public Runnable getRunnable(ExecutionTask executionTask){
		
		Runnable result = null;
		
		if(executionTask.getTaskType() == null){
			result = null;
		} else if((executionTask.getTaskType()).equalsIgnoreCase("BASIC")){
			result = new RunnableThreadBasic (executionTask);	     
		} else if((executionTask.getTaskType()).equalsIgnoreCase("SPREAD")){
			result = new RunnableThreadSpread (executionTask);	  
		} else if((executionTask.getTaskType()).equalsIgnoreCase("1212")){
			result = new RunnableThread1212 (executionTask);
		} else if((executionTask.getTaskType()).equalsIgnoreCase("1234")){
			result = new RunnableThread1234 (executionTask);
		} else if((executionTask.getTaskType()).equalsIgnoreCase("1212dos")){
			result = new RunnableThread1212dos (executionTask);
		} else if((executionTask.getTaskType()).equalsIgnoreCase("multiple")){
			result = new RunnableThreadMultiple (executionTask);
		} else if((executionTask.getTaskType()).equalsIgnoreCase("multiple-2")){
			result = new RunnableThreadMultiple2 (executionTask);
		}
		return result;
	}
}
