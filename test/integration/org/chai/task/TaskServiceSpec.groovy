package org.chai.task

import grails.test.mixin.TestFor;

import org.chai.task.Task.TaskStatus;

class TaskServiceSpec extends IntegrationTests {

	def taskService
	
	def "handle message sets status to completed"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.NEW, dataId: 1).save(failOnError:true)
		
		when:
		taskService.handleMessage(task.id)
		
		then:
		Task.list()[0].status == TaskStatus.COMPLETED
	}
	
	def "handle message increments number of tries"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.NEW, dataId: 1).save(failOnError:true)
		
		when:
		taskService.handleMessage(task.id)
		
		then:
		Task.list()[0].numberOfTries == 1
	}
	
	def "send to queue sets senttoqueue"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.NEW, dataId: 1).save(failOnError:true)
		
		when:
		taskService.metaClass.rabbitSend = { Object[] args ->  }
		taskService.sendToQueue(task)
		
		then:
		Task.list()[0].sentToQueue == true
	
		// TODO somehow it does not work to override a metaClass method twice inside the same test
//		when:
//		taskService.metaClass.rabbitSend = { Object[] args -> throw new RuntimeException() }
//		taskService.sendToQueue(task)
//		
//		then:
//		Task.list()[0].sentToQueue == false
	}	

	def "execute task does not do anything if task is completed"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.COMPLETED, dataId: 1).save(failOnError:true)
		task.metaClass.executeTask {throw new RuntimeException()}
		
		when:
		taskService.executeTask(task.id)
		
		then:
		notThrown RuntimeException
	}
	
	def "execute task does not do anything if task is aborted"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.ABORTED, dataId: 1).save(failOnError:true)
		task.metaClass.executeTask {throw new RuntimeException()}
		
		when:
		taskService.executeTask(task.id)
		
		then:
		notThrown RuntimeException
	}
	
	def "task is set as aborted when abort exception is thrown"() {
		setup:
		def task = new DummyTask(principal: 'principal', status: TaskStatus.NEW, dataId: 1).save(failOnError:true)
		task.metaClass.executeTask {throw new TaskAbortedException()}
		
		when:
		taskService.executeTask(task.id)
		
		then:
		Task.list()[0].status == TaskStatus.COMPLETED
	}
}
