package org.chai.task

import grails.plugin.spock.IntegrationSpec;
import grails.test.mixin.TestFor;
import grails.validation.ValidationException;

import org.chai.task.Task.TaskStatus;

class TaskDomainSpec extends IntegrationSpec {

	def "null constraints"() {
		when:
		new DummyTask(principal: 'principal', status: TaskStatus.NEW, dataId: 1).save(failOnError: true)
		
		then:
		Task.count() == 1
		
		when:
		new DummyTask(status: TaskStatus.NEW, dataId: 1).save(failOnError: true)
		
		then:
		Task.count() == 2
		
		when:
		new DummyTask(principal: 'principal', dataId: 1).save(failOnError: true)
		
		then:
		thrown ValidationException
	}
	
	def "increment progress on aborted class throws exception"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.IN_PROGRESS, dataId: 1).save(failOnError: true)
		task.max = 1
		task.current = 0
		task.aborted = true
		
		when:
		task.incrementProgress()
		
		then:
		thrown TaskAbortedException
	}
	
	def "abort"() {
		setup:
		def task
		Task.withNewTransaction {
			task = new DummyTask(principal: 'principal', status: TaskStatus.IN_PROGRESS, dataId: 1).save(failOnError: true)
		}
		
		when:
		task.abort()
		
		then:
		task.aborted == true
		
		cleanup:
		Task.withNewTransaction {
			DummyTask.list().each{it.delete(flush: true)}
		}
	}
	
	def "set maximum"() {
		setup:
		def task
		Task.withNewTransaction {
			task = new DummyTask(principal: 'principal', status: TaskStatus.IN_PROGRESS, dataId: 1).save(failOnError: true)
		}
		
		when:
		task.setMaximum(100)
		
		then:
		task.current == 0
		task.max == 100
		
		cleanup:
		Task.withNewTransaction {
			DummyTask.list().each{it.delete(flush: true)}
		}
	}
	
	def "increment progress"() {
		setup:
		def task
		Task.withNewTransaction {
			task = new DummyTask(principal: 'principal', status: TaskStatus.IN_PROGRESS, dataId: 1).save(failOnError: true)
			task.max = 1
			task.current = 0
		}
		
		when:
		task.incrementProgress()
		
		then:
		task.current == 1
		
		cleanup:
		Task.withNewTransaction {
			DummyTask.list().each{it.delete(flush: true)}
		}
	}
	
}
