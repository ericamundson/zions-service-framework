package com.zions.mr.permissions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.vsts.services.work.WorkManagementService
import com.zions.webbot.cli.CliWebBot
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.notification.NotificationService
import com.zions.auto.base.CompletedSteps
import com.zions.auto.pages.LoginPage
import com.zions.auto.pages.MainHeader
import com.zions.auto.pages.MrProjectSettingsPage

import groovy.util.logging.Slf4j
import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.NoSuchElementException
import java.util.concurrent.TimeUnit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

@Component
@Slf4j
class SetMrPermissions  implements CliWebBot {
	// Failure types
	static String LOGIN_FAILURE = 'ADO login failure'
	
	def steps = new CompletedSteps()
	def projectsToProcess = []
	def history
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	LoginPage loginPage
	
	@Autowired
	MainHeader adoHeader
	
	@Autowired
	MrProjectSettingsPage mrSettingsPage

	@Value('${email.recipient.addresses:}')
	private String[] recipientEmailAddresses

	@Value('${cache.filename:"history.json"}')
	String cacheFilename
	
	@Value('${cache.dir:"c:/ModernRequirementsPermissions"}')
	String cacheDir

	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.areapath:}')
	String areapath

	@Value('${tfs.owner:}')
	String owner

	public boolean checkPreconditions(ApplicationArguments data) {
		// Get projects already processed by previous executions
		history = new HistoryCache()
		
		// Get all projects for Org
		def projects = projectManagementService.getProjects(collection)
		
		// Get list of projects that have not yet been processed
		projectsToProcess = history.getProjectsToProcess(projects.value)
		
		if (projectsToProcess.size() == 0) {
			log.info("No more projects to process.  To process all projects, delete the history cache: $cacheFilename")
			return false
		}
		else
			return true
	}
	
	public def execute(ApplicationArguments data, WebDriver driver, WebDriverWait wait) {

		//******** Log into ADO ******
		loginPage.set(driver, wait, steps)
		if (!loginPage.login()) {
			reportError(driver, loginPage.error,LOGIN_FAILURE)
			return
		}
		
		// For each project to be processed, set Modern Requirements settings for the 3 MR groups
		mrSettingsPage.set(driver, wait, steps)
		projectsToProcess.each { project ->
			// For each project, go to MR Settings page to update permissions
			String pName = "${project.name}"
			log.info("*** Processing permissions updates for project: $pName")
			if (!mrSettingsPage.go(pName)) {
				log.error("Could not load project settings page for project $project: ${mrSettingsPage.error.message}")
				return
			}
			mrSettingsPage.enterGroupPermissions('MRUsers',['Allow','Deny','Allow','Deny','Deny','Allow','Deny','Deny'])
			mrSettingsPage.enterGroupPermissions('MRAdmin',['Allow','Allow','Allow','Allow','Allow','Allow','Allow','Allow'])
			mrSettingsPage.enterGroupPermissions('MRPowerUsers',['Allow','Deny','Allow','Deny','Allow','Allow','Allow','Allow'])
			
			history.save(pName)
		}
		
		// Log out of ADO
		adoHeader.set(driver, wait, steps)
		if (!adoHeader.signout()) 
			log.error("WARNING: Error loging out of ADO: ${adoHeader.error}")
			
		// Success!!!
		steps.completedSteps.each { step -> println(step) }
		println(steps.formatForLog())
		
	}
	private void reportError(WebDriver driver, Exception e, String newFailType) {
		log.error("$newFailType: ${e.message}")

	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	// Supporting Class that holds persistent monitoring status across invocations
	class HistoryCache {
		def completedProjects = []
		String cachePath
		public HistoryCache() {
			try {
				cachePath = cacheDir + cacheFilename
				File file = new File(cachePath)
				if (file.exists()) {
					BufferedReader reader = file.newReader()
					def data = reader.text
					reader.close()
					def parsed = new JsonSlurper().parseText(data)
					this.completedProjects = parsed.projects
				}
			} catch (e) {
				log.error("An error occurred reading cached status: $e.message")
			}
		}
		boolean save(String projectName) {
			completedProjects.add(projectName)
			
			boolean saved = false
			try {
				FileWriter myWriter = new FileWriter(cachePath)
				def output = [projects: completedProjects]
				myWriter.write(new JsonBuilder(output).toPrettyString())
				myWriter.flush()
				myWriter.close()
				saved = true
			} catch (IOException e) {
				log.error("An error occurred writing to cache file '$cachePath'.  Error: $e.message");
			}
			return saved
		}
		boolean delete() {
			boolean deleted = true
			try {
				File cacheFile = new File(cachePath)
				if (cacheFile.exists()) {
					if (!cacheFile.delete()) {
						log.error("Unable to delete status file: $cacheFile.path")
						deleted = false
					}
				}
			} catch (e) {
				log.error("An error occurred deleting status file: $e.message");
			}
			return deleted
		}
		def getProjectsToProcess(def projects) {
			def projectsToProcess = []
			projects.each { project ->
				if (!completedProjects.contains(project.name))
					projectsToProcess.add(project)
			}
			return projectsToProcess
		}
	}
}

