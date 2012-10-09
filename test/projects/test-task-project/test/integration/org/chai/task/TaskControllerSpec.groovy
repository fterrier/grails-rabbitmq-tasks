package org.chai.task

import grails.plugin.spock.IntegrationSpec;

import javax.servlet.ServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.util.WebUtils;

class TaskControllerSpec extends IntegrationSpec {

	def taskController
	
	def "creating a task sets the shiro principal"() {
		setup:
		taskController = new TaskController()
		def user = new ShiroUser(username: 'principal', passwordHash: '123').save(failOnError: true)
		def subject = [getPrincipal: { user?.username }, isAuthenticated: { user==null?false:true }, login: { token -> null }] as Subject
		ThreadContext.put( ThreadContext.SECURITY_MANAGER_KEY, [ getSubject: { subject } ] as SecurityManager )
		SecurityUtils.metaClass.static.getSubject = { subject }
		WebUtils.metaClass.static.getSavedRequest = { ServletRequest request -> null }
		
		when:
		taskController.params['class'] = 'DummyTask'
		taskController.params['dataId'] = 1
		taskController.create()
		
		then:
		Task.count() == 1
		Task.list()[0].principal == 'principal'
		taskController.response.redirectedUrl == '/'
	}
	
}
