class TasksGrailsPlugin {
    // the plugin version
    def version = "0.4"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [
		"quartz":"1.0-RC2",
		"rabbitmq":"1.0.0.RC2",
		"shiro":"1.1.5"
	]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
		"grails-app/views/*",
		"grails-app/domain/org/chai/task/DummyFileTask.groovy",
		"grails-app/domain/org/chai/task/DummyTask.groovy"
    ]

    // TODO Fill in these fields
    def title = "Tasks Plugin" // Headline display name of the plugin
    def author = "François Terrier"
    def authorEmail = "fterrier@gmail.com"
    def description = '''\
The tasks plugin provides a way to run background tasks in grails using 
rabbitmq to queue them. Provides a framework for adding tasks, deleting 
them, and aborting them.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/tasks"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "BSD3"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

}
