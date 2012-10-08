package org.chai.task

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.SecurityUtils;
import org.chai.task.Task.TaskStatus
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.springframework.web.multipart.MultipartFile

class TaskController {
	
	def grailsApplication
	def taskService
	
	def getTargetURI() {
		return params.targetURI?: "/"
	}
	
	/**
	* Lists all tasks currently saved in the database
	*/
	def list = {
		params.max = Math.min(params.max ? params.int('max') : grailsApplication.config.site.entity.list.max, 100)
		params.offset = params.offset ? params.int('offset'): 0
	   
		def tasks = Task.list(params)
	   
		render (view: '/task/list', model:[tasks: tasks])
	}
	
	/**
	 * Ajax call that gets the progress of an individual task
	 */
	def progress = {
		if (log.isDebugEnabled()) log.debug("task.progress, params:"+params)
		
		def taskIds = params.list('ids')
		def taskList = taskIds.collect {Task.get(it)}
		taskList = taskList - null
		
		render(contentType:"text/json") {
			tasks = array {
				taskList.each { task ->
					t (
						id: task.id,
						status: task.status.name(),
						progress: task.retrievePercentage()
					)
				}
			}
		}
	}
	
	/**
	 * Displays the task creation form according to the specified task class
	 */
	def taskForm = {
		Class taskClass = getTaskClass()
		if (taskClass != null) {
			def task = taskClass.newInstance()
			render (view: '/task/'+task.getFormView(), model: task.getFormModel())
		}
		else {
			response.sendError(404)
		}
	}
	
	/**
	 * Creates a task from a form, renders the form if the task is not valid, 
	 * redirects to targetURI otherwise. Expects a file, and creates a temp file.
	 */
	def createTaskWithFile = { TaskWithFileCommand cmd ->
		if (log.isDebugEnabled()) log.debug("task.createTaskWithFile, params:"+params)
		
		Class taskClass = getTaskClass()
		if (taskClass == null) {
			response.sendError(404)
		}
		else {
			def task = taskClass.newInstance()
			def hasErrors = false
			if (!cmd.hasErrors()) {
				task.inputFilename = cmd.file.originalFilename
				
				task.properties = params
				if (create(task)) {
					// create input file and copy content to file on disk
					def file = new File(task.folder, task.inputFilename)
					cmd.file.transferTo(file)
				}
				else {
					hasErrors = true
				}
			}
			else {
				hasErrors = true
				
				task.properties = params
				fillFields(task)
				task.validate()
			}
						
			if (hasErrors) {
				if (log.debugEnabled) log.debug('validation errors, cmd: '+cmd.errors+', task: '+task.errors)
				
				if (task.errors.hasFieldErrors('inputFilename')) cmd.errors.addError(task.errors.getFieldError('inputFilename'))
				
				def model = task.getFormModel()
				model.taskWithFile = cmd
				render (view: '/task/'+task.getFormView(), model: model)
			}
			else redirect(uri: targetURI)
		}
	}
	
	def downloadOutput = {
		def task = Task.get(params.int('id'))
		if (task != null && task.status == TaskStatus.COMPLETED && task.outputFilename != null) {
			def file = new File(task.getFolder(), task.outputFilename)
			if (!file.exists()) {
				flash.message = message(code: 'task.output.not.found')
			}
			else {
				def zipFile = getZipFile([file], 'output.zip')
				
				if(zipFile.exists()){
					response.setHeader("Content-disposition", "attachment; filename=" + zipFile.getName());
					response.setContentType("application/zip");
					response.setHeader("Content-length", zipFile.length().toString());
					response.outputStream << zipFile.newInputStream()
				}
			}
		}
		else {
			response.sendError(404)
		}
	}
	
	/**
	 * Creates a task from a form, renders the form if the task is not valid, 
	 * redirects to targetURI otherwise
	 */
	def createTask = {
		throw new NotImplementedException()
	}
	
	/**
	 * Silently creates a task and sends it for processing
	 */
	def create = {
		if (log.isDebugEnabled()) log.debug("task.create, params:"+params)
		
		Class taskClass = getTaskClass()
		if (taskClass != null) {
			def task = taskClass.newInstance()
			
			task.properties = params
			def valid = create(task)
			if (!valid && !flash.message) flash.message = message(code: 'task.creation.validation.error')
			
			redirect(uri: targetURI)
		}
		else {
			response.sendError(404)
		}
	}
	
	private def getTaskClass() {
		Class taskClass
		try {
			if (params.get('class') != null) taskClass = Class.forName('org.chai.task.'+params['class'], true, Thread.currentThread().contextClassLoader)
		} catch (ClassNotFoundException e) {
			if (log.isWarnEnabled()) log.warn('class not found with name: '+params['class'])
		}
		return taskClass
	}
	
	private def fillFields(def task) {
		// we fill the default fields
		task.status = TaskStatus.NEW
		task.principal = SecurityUtils.subject.principal
		task.added = new Date()
	}
	
	private def create(def task) {
		// we set the fields
		fillFields(task)
		
		if (task.validate()) {
			if (log.isDebugEnabled()) log.debug ("task is valid, checking uniqueness")
			
			// we check that it doesn't already exist
			if (!task.isUnique()) {
				flash.message = message(code: 'task.creation.notunique.error', args: [createLink(controller: 'task', action: 'list')])
				return false;
			}
			else {
				// we save it
				task.save(failOnError: true)
				
				// we send it for processing
				taskService.sendToQueue(task)
				
				// we redirect to the list
				flash.message = message(code: 'task.creation.success', args: [createLink(controller: 'task', action: 'list')])
				return true;
			}
		}
		else {
			if (log.isInfoEnabled()) log.info ("validation error in ${task}: ${task.errors}}")
			return false;
		}
	}
	
	def purge = {
		def tasks = Task.findAllByStatusInList([TaskStatus.COMPLETED, TaskStatus.ABORTED])
		
		tasks.each { task -> 
			task.cleanTask()
			task.delete()
		}
		redirect(action: 'list');
	}
	
	def delete = {
		if (log.isDebugEnabled()) log.debug("task.delete, params:"+params)
		
		def entity = Task.get(params.int('id'))
		if (entity != null) {
			if (entity.status != TaskStatus.IN_PROGRESS || !entity.sentToQueue) {
				try {
					entity.cleanTask()
					entity.delete()
					
					if (!flash.message) flash.message = message(code: 'default.deleted.message', args: [message(code: 'task.label', default: 'entity'), params.id])
					redirect(uri: targetURI)
				}
				catch (org.springframework.dao.DataIntegrityViolationException e) {
					flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'task.label', default: 'entity'), params.id])
					redirect(uri: targetURI)
				}
			}
			else {
				entity.abort()
				
				flash.message = message(code: 'task.aborting.message')
				redirect(uri: targetURI)
			}
		}
		else {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'task.label', default: 'entity'), params.id])
			redirect(uri: targetURI)
		}
	}
	
	public static File getZipFile(List<File> files, String filename) throws IOException {
		File zipFile = File.createTempFile(filename, '.zip');

		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
				
		try {
			for(File file: files){
				FileInputStream fileInputStream = new FileInputStream(file);
				ZipEntry zipEntry = new ZipEntry(file.getName());
				zipOutputStream.putNextEntry(zipEntry);
				
				IOUtils.copy(fileInputStream, zipOutputStream);
				zipOutputStream.closeEntry();
			}
		} catch (IOException e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(zipOutputStream);
			IOUtils.closeQuietly(zipOutputStream);
		}
			
		return zipFile;
	}
}

class TaskWithFileCommand {
	MultipartFile file
	
	static constraints = {
		file(blank:false, nullable:false)
	}
}
	