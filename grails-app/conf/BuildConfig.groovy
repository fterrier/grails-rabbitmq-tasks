grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
		grailsHome()
		grailsPlugins()
		grailsCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
		mavenCentral()
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.18'
		compile ('org.springframework.amqp:spring-rabbit:1.1.2.BUILD-SNAPSHOT') {
			excludes 'junit',
				'spring-aop',
				'spring-core', // Use spring-core from Grails.
				'spring-oxm',
				'spring-test',
				'spring-tx',
				'slf4j-log4j12',
				'log4j'
		}
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:2.0.3",
              ":rest-client-builder:1.0.2") {
            export = false
        }
			 
		compile (":hibernate:$grailsVersion") {
			export = false
		}
			  
		compile (":shiro:1.1.5")
		compile (":quartz:1.0-RC2")
		compile (":rabbitmq:1.0.0.RC2")
		
		test (":spock:0.6") {
			export = false
		}
    }
}
