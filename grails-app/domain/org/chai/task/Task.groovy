package org.chai.task

import java.io.File

import org.apache.commons.io.FileUtils

abstract class Task implements Progress {

	enum TaskStatus{NEW, COMPLETED, IN_PROGRESS, ABORTED}
	
	def grailsApplication
	
	String principal
	TaskStatus status
	Date added = new Date()
	
	Date started
	Date finished
	
	Integer numberOfTries = 0
	Boolean sentToQueue = false
	
	// progress
	Long max = 0;
	Long current = null;
	Boolean userAborted;
	
	abstract def executeTask()
	abstract boolean isUnique()
	
	String getFormView() {
		return null
	}
	
	Map getFormModel() {
		return null
	}
	
	String getOutputFilename() {
		return null
	}
	
	File getFolder() {
		def folder = new File(grailsApplication.config.task.temp.folder + File.separator + this.getId())
		if (!folder.exists()) folder.mkdirs()
		return folder
	}
	
	def cleanTask() {
		File folder = getFolder()
		if (folder != null && folder.exists()) FileUtils.deleteDirectory(folder)
	}
	
	void incrementProgress(Long increment = null) {
		if (log.isDebugEnabled()) log.debug('incrementProgress, max: '+ max +', current: '+current)
		
		if (current != null) {
			Task.withNewTransaction {
				if (userAborted) throw new TaskAbortedException()
				if (increment == null) current++
				else current += increment
				this.save(flush: true)
			}
		}
	}
	
	void setMaximum(Long max) {
		Task.withNewTransaction {
			this.max = max;
			this.current = 0;
			this.save(flush: true)
		}
	}
	
	void abort() {
		Task.withNewTransaction {
			userAborted = true
			this.save(flush: true)
		}
	}
	
	boolean isAborted() {
		return userAborted
	}
	
	Double retrievePercentage() {
		if (current == null || max == 0) return null
		return current.doubleValue()/max.doubleValue()
	}
	
	static mapping = {
		version false
	}
	
	static constraints = {
		principal(nullable: true)
		
		status(nullable: false)
		max(nullable: false)
		current(nullable: true)
		
		userAborted(nullable: true)
		
		started(nullable: true)
		finished(nullable: true)
		
		userAborted(nullable: true)
	}
	
}

