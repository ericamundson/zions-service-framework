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
	static String ADO_FAILURE = 'ADO site not available'
	static String SMARTDOC_FAILURE = 'SD page load failure'
	static String REVIEW_FAILURE = 'Review Request failure'
	
	// UI elements
	static String CONFIRM_BUTTON = "confirm-dialog-ok-button-rvm-req-confirmation-dialog"
	static String MR_LOGOUT_BUTTON = "smd_left_panel_footerbtn-container"
	
	// Tags
	static String FAILURE_TAG = 'CURRENT OUTAGE'
	static String SUCCESS_TAG = 'SUCCESSFUL RETEST'
	
	def steps = new CompletedSteps()
	def status
	
	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	WorkManagementService workManagementService

	@Autowired
	IAttachments attachmentService
	
	@Autowired
	NotificationService notificationService
	
	@Autowired
	LoginPage loginPage
	@Autowired
	MainHeader adoHeader
	@Autowired
	MrProjectSettingsPage mrSettingsPage

	@Value('${email.recipient.addresses:}')
	private String[] recipientEmailAddresses

	@Value('${cache.filename:"status.json"}')
	String cacheFilename
	
	@Value('${cache.dir:"c:/SmartDocMonitoring"}')
	String cacheDir

	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.areapath:}')
	String areapath

	@Value('${tfs.owner:}')
	String owner


	public MonitorSmartDoc() {
	}

	public def execute(ApplicationArguments data, WebDriver driver, WebDriverWait wait) {
		//******** Log into ADO ******
		loginPage.set(driver, wait, steps)
		if (!loginPage.login()) {
			reportError(driver, loginPage.error,LOGIN_FAILURE)
			return
		}
		
		// Get all projects for Org
		def projects = projectManagementService.getProjects(collection)
		mrSettingsPage.set(driver, wait, steps)
		projects.'value'.each { project ->
			// TODO: Do some code to write project stuff
			String pName = "${project.name}"
			if (!mrSettingsPage.go(pName)) {
				log.error("Could not load project settings page for project $project: ${mrSettingsPage.error.message}")
				return
			}
			mrSettingsPage.enterGroupPermissions('MRUsers',['Allow','Deny','Allow','Deny','Deny','Allow','Deny','Deny'])
			Thread.sleep(1000)
			mrSettingsPage.enterGroupPermissions('MRAdmin',['Allow','Allow','Allow','Allow','Allow','Allow','Allow','Allow'])
			Thread.sleep(1000)
			mrSettingsPage.enterGroupPermissions('MRPowerUsers',['Allow','Deny','Allow','Deny','Allow','Allow','Allow','Allow'])
			Thread.sleep(1000)
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
}

