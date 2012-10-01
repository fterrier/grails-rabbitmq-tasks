Tasks plugin
===

The tasks plugin provides a way to run background tasks in grails using [rabbitmq][rabbitmq] to queue them. Provides
a framework for adding tasks, deleting them, and aborting them. Provides the option to create tasks that take
a file as an input and that produce output files.
[rabbitmq]: http://www.rabbitmq.com/

Installation
---

To install this plugin, run the following:

		grails install-plugin tasks
		
You also need to install rabbitmq-server. To do that, follow the instructions on [rabbitmq.com](http://www.rabbitmq.com/).
		
Configuration
---
		
There is one configuration option that can be set to specify the directory for temporary output files:

		grails.tasks.task.temp.folder=/tmp 

Create a simple task with no input/output
---

To create a new task, you just need to extend the Task abstract class and provide an implementation for the ``executeTask()`` and ``isUnique()`` methods.

		class MyTask extends Task {
			
			void executeTask() {
				// code that runs the task
			}
			
			boolean isUnique() {
				// return true if there exists already a task
				// that will have the same effect as this one
				// otherwise return false
			}
			
		}

**The implementation should be in the package ``org.chai.task``**.

Once a task is succesfully created, it is persisted in the database, and automatically sent to rabbitmq if rabbitmq-server is running. Depending on rabbitmq configuration, the tasks are picked up by 1 or several workers and executed in the background.

If the server is stopped, tasks will be interrupted and restarted at startup. If rabbitmq is not running, a cron job will attend to send the tasks to the queue every 2 minutes.

Once you've added the code that runs the task, you can use actions of the ``TaskController`` to create and delete tasks. The controller provides the following action:

		def create = {
			// Creates a simple task:
			// - takes a 'class' parameter that specifies the
			// non-qualified name of the Task implementation class
			// - all other params will be bound to the Task 
			// implementation class using bindParams(…)
		}
		
Thus, calling ``task/create?class=MyTask`` will create an instance of the ``MyTask`` class, and will send it to rabbitmq for processing. When the task is picked up from the queue, the ``executeTask`` method will be run.

You can add an arbitrary number of named parameters to your class and pass them as a request parameter to the ``create`` action. For instance, let's modify the class as follows:

		class MyTask extends Task {
			
			Integer param1
			
			...
		}
		
Calling ``task/create?class=MyTask&param1=10`` will bind 10 to the new instance ``param1`` field.

Create a task that takes a file as an input
---

To create a task that takes a file as an input, you need to implement the 2 extra methods ``getFormView()`` and ``getFormModel()``, that specify where to find the view and the model for the form that will upload the file.

		class MyTask extends Task {
			
			String getFormView() {
				// returns the name of the view that will display the form
			}
			
			Map getFormModel() {
				// returns the model that will be used to display the form
			}
			
			...
		}
		
The view might look something like this:

		<g:form url="[controller:'task', action:'createTaskWithFile']">
			<input type="hidden" name="class" value="MyTask"/>
			<input  type="file" name="file" 
				value="${fieldValue(bean:task, field:'file')}"/>
			
			<button type="submit">Upload</button>
		</g:form>
		
Along with the following model:

		Map getFormModel() {
			return [task: this]
		}

The only restriction placed on the form view is that the form should contain an input field of type file named ``file``.
		
The ``TaskController`` provides two actions to display the upload form and to create the task.
		
		def taskForm = {
			// Displays the task creation form
			// - takes a 'class' parameter that specifies the
			// non-qualified name of the Task implementation class
		}
		
		def createTaskWithFile = {
			// Creates a task with a file from POSTing the
			// task creation form
			// - takes a 'class' parameter that specifies the
			// non-qualified name of the Task implementation class
			// - expects an input file named 'file' in the POST
			// - all other params are bound to the Task
			// implementation class using bindParams(…)
		}

Calling the ``task/taskForm?class=MyTask`` URL will simply display the specified form and model defined in the Task implementation. That form should POST to ``task/createTaskWithFile``. Submitting the form will then create an instance of the class and save the file in the temporary directory specified in the configuration (cf. Configuration above) and will send it to rabbitmq.
	
Create a task that produces an output file
---

The plugin provides a mechanism for creating output files. If the Task implementation class provides an implemenation for the ``getOutputFilename()`` method, it means the task produces an output file that can be retrieved under that name.

		String getOutputFilename() {
			// returns the name of a the output file
		}

It is the job of the task implementation to create the file and save the output to the file specified by the ``getOutputFilename()`` method. That file should be placed in the folder given by the Task abstract class ``getFolder()`` method. A Task that creates an output could look like this:

		def executeTask() {
			String outputContent = "this is the output"
			File outputFile = new File(getFolder(), getOutputFilename())
			outputFile.createNewFile()
			
			def fileWriter = new FileWriter(outputFile)
			IOUtils.write(errorOutput, fileWriter)
			fileWriter.flush()
			IOUtils.closeQuietly(fileWriter)
		}

The ``TaskController`` provides a method to download the output file, zipped.
		
		def downloadOutput = {
			// If the task creates an output file, this sends the
			// output as a zip file to the outputStream.
			// - takes a 'id' parameter that specify the task for which
			// the output should be downloaded
		}
		
Calling ``task/downloadOutput?id=<task_id>`` will download the file.

Task status and progress
---

At any time, the status of a task can be retrieved by calling ``task.status``. There are 4 possible statuses, ``NEW``, ``COMPLETED``, ``IN_PROGRESS`` or ``ABORTED``. When a task is first created, its status is set to ``NEW``. Once it's picked up by the rabbitmq worker, it's set to ``IN_PROGRESS``. Once a task is finished, its status is set to ``COMPLETED``. If the task ``abort()`` method is called, the status is set to ``ABORTED``.

Beside a status, the Task class provides the functionality to set and increment a progress while the task is running. The Task class provides the following 2 methods:

		void setMaximum(Long max) {
			// sets the maximum progress of this task to 'max'
		}

		void incrementProgress(Long increment = null) {
			// increment the progress of that task by 'increment', or
			// by 1, if 'increment' is null
		}
		
It is obviously the job of the implementation to set the maximum number of steps it will take to reach 100% completion, as well as to increment the progress. The ``TaskController`` provides an action to interrogate the progress of 1 or several tasks:

		def progress = {
			// Returns a JSON response with the progress 
			// for the specified tasks. The response has the format
			// [
			//   {id: <task_id>, status: <task_status>, progress: <task_progress}, 
			//   {…}
			// ]
			// - takes a 'ids' parameter specifying a list of tasks
			// to query for progress
		}
		
Deleting tasks
---
	
Tasks that are not currently running (the status is not ``IN_PROGRESS``) can be deleted from the database using the ``TaskController`` delete action.
		
		def delete = {
			// Deletes the specified task
			// - takes a 'id' parameter that specify the task that should
			// be deleted
		}
		
Using this action, a task is removed from the database, but not from the rabbitmq queue. However, when the worker picks up the tasks, it will notice that the task has been deleted and will not execute it.

Another action allows one to delete all ``COMPLETED`` tasks from the database.
		
		def purge = {
			// Purges all COMPLETED tasks
		}

License
---

The task plugin is licensed under the terms of the [BSD 3-clause License][BSD 3-clause License].
[BSD 3-clause License]: http://www.w3.org/Consortium/Legal/2008/03-bsd-license.html