class RabbitmqTasksGrailsPlugin {
    // the plugin version
    def version = "0.4"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
		"grails-app/views/*",
		"grails-app/domain/org/chai/task/DummyFileTask.groovy",
		"grails-app/domain/org/chai/task/DummyTask.groovy"
    ]

    // TODO Fill in these fields
    def title = "Rabbitmq Tasks Plugin" // Headline display name of the plugin
    def author = "François Terrier"
    def authorEmail = "fterrier@gmail.com"
    def description = '''\
The tasks plugin provides a way to run background tasks in grails using 
rabbitmq to queue them. Provides a framework for adding tasks, deleting 
them, and aborting them.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rabbitmq-tasks"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "BSD3"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Clinton Health Access Initiative", url: "http://www.clintonhealthaccess.org/" ]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPRMQTASKS" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/fterrier/grails-rabbitmq-tasks/" ]

}
