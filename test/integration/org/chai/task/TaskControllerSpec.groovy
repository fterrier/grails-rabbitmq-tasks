package org.chai.task

import grails.converters.JSON;

import grails.plugin.spock.IntegrationSpec;
import org.chai.task.Task.TaskStatus;
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile;
import org.codehaus.groovy.grails.web.json.JSONObject;

class TaskControllerSpec extends IntegrationSpec {

	def taskController
	
	def "create task"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.params['dataId'] = 1
		taskController.create()
		
		then:
		Task.count() == 1
		Task.list()[0].dataId == 1
		Task.list()[0].max == 0
		Task.list()[0].current == null
		taskController.response.redirectedUrl == '/'
	}
	
	def "test task controller sets user identity"() {
		setup:
		taskController = new TaskController()
		taskController.metaClass.getSecurityIdentity = {return 'principal'}
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.params['dataId'] = 1
		taskController.create()
		
		then:
		Task.count() == 1
		Task.list()[0].principal == 'principal'
		taskController.response.redirectedUrl == '/'
	}
	
	def "create non-unique task"() {
		setup:
		new DummyTask(dataId: 1, principal: 'user', status: TaskStatus.NEW).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.params['dataId'] = 1
		taskController.create()
		
		then:
		Task.count() == 1
		taskController.response.redirectedUrl == '/'
	}
	
	def "create unique task that is a new task of an already completed one"() {
		setup:
		new DummyTask(dataId: 1, principal: 'user', status: TaskStatus.COMPLETED).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.params['dataId'] = 1
		taskController.create()
		
		then:
		Task.count() == 2
		taskController.response.redirectedUrl == '/'
	}
	
	def "create task validation"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.create()
		
		then:
		Task.count() == 0
		taskController.response.redirectedUrl == '/'
	}
	
	def "create task with wrong class"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'Inexistant'
		taskController.create()
		
		then:
		taskController.modelAndView == null
	}
	
	def "delete task"() {
		setup:
		def task = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.NEW, sentToQueue: false).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params.id = task.id
		taskController.delete()
		
		then:
		Task.count() == 0
		taskController.response.redirectedUrl == '/'
	}
	
	def "delete complete task"() {
		setup:
		def task = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.COMPLETED, sentToQueue: true).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params.id = task.id
		taskController.delete()
		
		then:
		Task.count() == 0
		taskController.response.redirectedUrl == '/'
	}
	
	// cannot test because of withNewTransaction call, and abort()
	// seems to not be overridable using metaClass (task.metaClass.abort = {aborted = true})
//	def "delete in progress task already sent aborts the task"() {
//		setup:
//		def user = newUser('user', 'uuid')
//		def task = new TestDummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.IN_PROGRESS, sentToQueue: true).save(failOnError: true)
//		taskController = new TaskController()
//		
//		when:
//		taskController.params.id = task.id
//		taskController.delete()
//		
//		then:
//		Task.count() == 1
//		Task.list()[].aborted == true
//		taskController.response.redirectedUrl == '/'
//	}
	
	
	def "delete new task already sent aborts the task"() {
		setup:
		def task = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.NEW, sentToQueue: true).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params.id = task.id
		taskController.delete()
		
		then:
		Task.count() == 0
		taskController.response.redirectedUrl == '/'
	}
	
	def "delete inexistant task"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params.id = '1'
		taskController.delete()
		
		then:
		Task.count() == 0
		taskController.response.redirectedUrl == '/'
	}
	
	def "purge tasks"() {
		setup:
		def task1 = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.NEW).save(failOnError: true)
		def task2 = new DummyTask(dataId: 2, principal: 'uuid', status: TaskStatus.COMPLETED).save(failOnError: true)
		def task3 = new DummyTask(dataId: 3, principal: 'uuid', status: TaskStatus.IN_PROGRESS).save(failOnError: true)
		def task4 = new DummyTask(dataId: 4, principal: 'uuid', status: TaskStatus.COMPLETED).save(failOnError: true)
		def task5 = new DummyTask(dataId: 5, principal: 'uuid', status: TaskStatus.ABORTED).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.purge()
		
		then:
		Task.count() == 2
		Task.list()[0].status == TaskStatus.NEW
		Task.list()[1].status == TaskStatus.IN_PROGRESS
	}
	
	def "progress when new"() {
		setup:
		def task1 = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.NEW).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['ids'] = [task1.id]
		taskController.progress()
		def content = taskController.response.contentAsString
		def jsonResult = JSON.parse(content)
		
		then:
		jsonResult.tasks.size() == 1
		jsonResult.tasks[0].status == "NEW"
		jsonResult.tasks[0].progress == JSONObject.NULL
		jsonResult.tasks[0].id == task1.id
	}
	
	def "progress when in progress"() {
		setup:
		def task1 = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.IN_PROGRESS, max: 100, current: 50).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['ids'] = [task1.id]
		taskController.progress()
		def jsonResult = JSON.parse(taskController.response.contentAsString)
		
		then:
		jsonResult.tasks.size() == 1
		jsonResult.tasks[0].status == "IN_PROGRESS"
		jsonResult.tasks[0].progress == 0.5
		jsonResult.tasks[0].id == task1.id
	}
	
	def "progress when task does not exist"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['ids'] = ['1']
		taskController.progress()
		def jsonResult = JSON.parse(taskController.response.contentAsString)
		
		then:
		jsonResult.tasks.size() == 0
	}
	
	
	def "task form with inexisting class"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'not_existant'
		taskController.taskForm()
		
		then:
		taskController.response.redirectedUrl == null
	}
	
	def "task form"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyFileTask'
		taskController.taskForm()
		
		then:
		taskController.modelAndView.model.task != null
		taskController.modelAndView.viewName == '/task/dummyFile'		
	}
	
	def "create task with file - normal behaviour"() {
		setup:
		File tempFile = new File("test/integration/org/chai/task/testFile.csv")
		GrailsMockMultipartFile grailsMockMultipartFile = new GrailsMockMultipartFile(
			"testFile", "testFile.csv", "", tempFile.getBytes())
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyFileTask'
		taskController.params.dataId = '1'
		taskController.params.file = grailsMockMultipartFile
		taskController.createTaskWithFile()
		
		then:
		taskController.response.redirectedUrl == '/'
		Task.count() == 1
		Task.list()[0].dataId == 1
//		Task.list()[0].encoding == 'UTF-8'
//		Task.list()[0].delimiter == ','
		Task.list()[0].inputFilename == 'testFile.csv'
		
	}
	
	def "create task with file validation - no fields"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyFileTask'
		taskController.createTaskWithFile()
		
		then:
		taskController.modelAndView.model.task != null
		taskController.modelAndView.model.taskWithFile != null
		taskController.modelAndView.viewName == '/task/dummyFile'
		taskController.modelAndView.model.taskWithFile.errors.getFieldErrors('file').size() == 1
		taskController.modelAndView.model.taskWithFile.errors.getFieldErrors('inputFilename').size() == 1
		taskController.modelAndView.model.task.errors.getFieldErrors('dataId').size() == 1
		Task.count() == 0
	}
	
	def "create task with file validation - file name not correct"() {
		setup:
		File tempFile = new File("test/integration/org/chai/task/testFile.csv")
		GrailsMockMultipartFile grailsMockMultipartFile = new GrailsMockMultipartFile(
			"testFile", "testFile.wrong", "", tempFile.getBytes())
		taskController = new TaskController()
		
		when:
		taskController.params['class'] = 'DummyFileTask'
		taskController.params.encoding = 'UTF-8'
		taskController.params.delimiter = ','
		taskController.params.file = grailsMockMultipartFile
		taskController.createTaskWithFile()
		
		then:
		taskController.modelAndView.model.task != null
		taskController.modelAndView.model.taskWithFile != null
		taskController.modelAndView.viewName == '/task/dummyFile'
		taskController.modelAndView.model.taskWithFile.errors.getFieldErrors('file').size() == 0
		taskController.modelAndView.model.taskWithFile.errors.getFieldErrors('inputFilename').size() == 1
		taskController.modelAndView.model.task.errors.getFieldErrors('dataId').size() == 1
		Task.count() == 0
	}
	
	def "download output when task class does not exist"() {
		setup:
		taskController = new TaskController()
		
		when:
		taskController.params['id'] = 1
		taskController.downloadOutput()
		
		then:
		taskController.response.redirectedUrl == null
	}
	
	def "download output when task is not completed"() {
		setup:
		def task = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.NEW).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['id'] = task.id
		taskController.downloadOutput()
		
		then:
		taskController.response.redirectedUrl == null
	}
	
	def "download output when task has no output"() {
		setup:
		def task = new DummyTask(dataId: 1, principal: 'uuid', status: TaskStatus.COMPLETED).save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['id'] = task.id
		taskController.downloadOutput()
		
		then:
		taskController.response.redirectedUrl == null
	}
	
	def "download output when output file not found"() {
		setup:
		def task = new DummyFileTask(dataId: 1, periodId: 1, principal: 'uuid', status: TaskStatus.COMPLETED, inputFilename: 'test.csv', delimiter: ',', encoding: 'UTF-8').save(failOnError: true)
		taskController = new TaskController()
		
		when:
		taskController.params['id'] = task.id
		taskController.downloadOutput()
		
		then:
		taskController.flash.message != null
		taskController.response.contentType != "application/zip";
	}
	
	def "download output when output file found"() {
		setup:
		def task = new DummyFileTask(dataId: 1, periodId: 1, principal: 'uuid', status: TaskStatus.COMPLETED, inputFilename: 'testFile.csv', delimiter: ',', encoding: 'UTF-8').save(failOnError: true)
		task.metaClass.getFolder = { return new File('test/integration/org/chai/task/') }
		task.metaClass.getOutputFilename = { return "testFile.csv"}
		taskController = new TaskController()
		
		when:
		taskController.params['id'] = task.id
		taskController.downloadOutput()
		
		then:
		taskController.flash.message == null
		taskController.response.outputStream != null
		taskController.response.contentType == "application/zip";
	}
	
	
}
