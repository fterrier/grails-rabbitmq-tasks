package org.chai.task

import org.chai.task.Task;
import org.chai.task.Task.TaskStatus;
import org.springframework.amqp.core.Message;

class TaskService {
	
	static rabbitQueue = 'adminQueue'
	
	static transactional = false
	
	def handleMessage(Long taskId) {
		if (log.isDebugEnabled()) log.debug('handleMessage(Long taskId='+taskId+')')
		
		executeTask(taskId)
	}
	
	def handleMessage(Object taskId) {
		if (log.isDebugEnabled()) log.debug('handleMessage(Object taskId='+taskId+')')
		
		executeTask(taskId)
	}
	
	void handleMessage(String taskId) {
		if (log.isDebugEnabled()) log.debug('handleMessage(String taskId='+taskId+')')
		
		executeTask(taskId)
	}
	
	def executeTask(Long taskId) {
		// handle Long message…
		def task
		Task.withTransaction {
			task = Task.get(taskId)
		}
		
		if (task != null && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.ABORTED) {
			if (!task.userAborted) {
				// we set the status to in_progress
				Task.withTransaction {
					task.status = TaskStatus.IN_PROGRESS
					task.numberOfTries++
					task.started = new Date()
					task.save(failOnError: true)
				}
				
				// we execute the task
				try {
					task.executeTask()
				} 
				catch (TaskAbortedException e) {
					if (log.isInfoEnabled()) log.info('task userAborted: '+task)
				}
			}
			
			Task.withTransaction {
				// we set the status to complete or userAborted
				if (task.userAborted) task.status = TaskStatus.ABORTED
				else task.status = TaskStatus.COMPLETED
				
				task.finished = new Date()
				task.save(failOnError: true)
			}
		}
	}
	
	def sendToQueue(def task) {
		if (log.isDebugEnabled()) log.debug("sendToQueue(task="+task+")")
		
		Task.withTransaction {
			task.sentToQueue = false
			try {
				// we add the class to the queue for processing
				rabbitSend 'adminQueue', task.id
				// we set the flag to true, the queue now is 
				// responsible for making sure the task gets 
				// processed (even if the queue fails, it should persist the job
				task.sentToQueue = true
			} catch (Exception e) {
				if (log.isWarnEnabled()) log.warn("exception trying to send the task to the queue for processing", e);
			}
		
			task.save(failOnError: true)
		}
	}

}
