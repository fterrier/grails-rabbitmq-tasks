package org.chai.task

import org.chai.task.Task.TaskStatus;
import org.springframework.context.i18n.LocaleContextHolder;

class DummyTask extends Task {

	Integer dataId
	
	public DummyTask() {
		super();
	}
	
	def executeTask() {
		
	}
	
	String getInformation() {
		
	}
	
	boolean isUnique() {
		def task = DummyTask.findByDataId(dataId)
		return task == null || task.status == TaskStatus.COMPLETED || task.status == TaskStatus.ABORTED
	}
	
	def cleanTask() {
		// nothing to do here
	}
	
	String getOutputFilename() {
		return null
	}
	
	String getFormView() {
		return null
	}
	
	Map getFormModel() {
		return null
	}
	
	static constraints = {
		dataId(nullable:false)
	}
	
}
