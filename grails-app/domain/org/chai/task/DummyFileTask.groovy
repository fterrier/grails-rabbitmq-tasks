package org.chai.task

class DummyFileTask extends Task {

	String inputFilename
	Long dataId
	
	String getInformation() {
	}
	
	String getFormView() {
		return 'dummyFile'	
	}
	
	String getOutputFilename() {
		return 'importOutput.txt'
	}
	
	def executeTask() {
	
	}

	boolean isUnique() {
		return true;
	}
	
	Map getFormModel() {
		return [
			task: this
		]
	}
	
	static constraints = {
		inputFilename(blank:false, nullable: false, validator: {val, obj ->
			return val.substring(val.lastIndexOf(".") + 1) == 'csv'
		})
	}
}
