package org.chai.task

import org.chai.task.Task.TaskStatus;
import grails.plugin.spock.IntegrationSpec;

class TaskJobSpec extends IntegrationSpec {

	def taskJob
	def taskService
	
	def "job sends all non sent to queue to the queue"() {
		setup:
		taskService.metaClass.rabbitSend = { Object[] args -> return; }
		taskJob = new TaskJob()
		taskJob.taskService = taskService
		
		when:
		def task1 = new DummyTask(principal: 'uuid', status: TaskStatus.NEW, dataId: '1', sentToQueue: false).save(failOnError:true)
		def task2 = new DummyTask(principal: 'uuid', status: TaskStatus.NEW, dataId: '2', sentToQueue: false).save(failOnError:true)
		taskJob.execute()
		
		then:
		task1.sentToQueue == true
		task2.sentToQueue == true
	}
	
}
