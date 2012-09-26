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
	
}
