package com.zions.mr.monitor.smartdoc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.notification.NotificationService

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
class MonitorSmartDoc  implements CliAction {
	// Failure types
	static String LOGIN_FAILURE = 'ADO login failure'
	static String ADO_FAILURE = 'ADO site not available'
	static String SMARTDOC_FAILURE = 'SD page load failure'
	static String REVIEW_FAILURE = 'Review Request failure'
	
	// UI elements
	static String LOGIN_BUTTON = 'idSIButton9'
	static String PASSWORD_FIELD = 'i0118'
	static String USERID_FIELD = 'i0116'
	static String CONFIRM_BUTTON = "confirm-dialog-ok-button-rvm-req-confirmation-dialog"
	static String MR_LOGOUT_BUTTON = "smd_left_panel_footerbtn-container"
	
	// Tags
	static String FAILURE_TAG = 'CURRENT OUTAGE'
	static String SUCCESS_TAG = 'SUCCESSFUL RETEST'
	
	def completedSteps = []
	def startTime = (new Date().getTime())
	long elapsedSec = 0
	def status
	
	@Autowired
	WorkManagementService workManagementService

	@Autowired
	IAttachments attachmentService
	
	@Autowired
	NotificationService notificationService

	@Value('${email.recipient.addresses:}')
	private String[] recipientEmailAddresses

	@Value('${cache.filename:"status.json"}')
	String cacheFilename
	
	@Value('${cache.dir:"c:/SmartDocMonitoring"}')
	String cacheDir
	
	@Value('${maint.window:}')
	String maintWindow // format is HH:MM-HH:MM

	@Value('${tfs.project:}')
	String project

	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.areapath:}')
	String areapath

	@Value('${tfs.owner:}')
	String owner

	@Value('${mr.url}')
	String mrUrl

	@Value('${mr.smartdoc.name}')
	String smartDocName

	@Value('${ms.login}')
	String msLogin
	
	@Value('${tfs.url}')
	String tfsUrl
	
	@Value('${tfs.user}')
	String tfsUser

	@Value('${tfs.password}')
	String tfsPassword

	@Value('${sel.timeout.sec}')
	int waitTimeoutSec
	
	@Value('${sel.toomany.sec}')
	int tooManySec
	
	@Value('${ticket.creation.count}')
	int ticketCount

	@Value('${mr.haslicense}')
	boolean hasLicense

	public MonitorSmartDoc() {
	}

	public def execute(ApplicationArguments data) {
		
		// Don't process if during maintenance window
		MaintenanceWindow window = new MaintenanceWindow(maintWindow)
		if ( window.isActive()) {
			log.info('In maintenance window.  No monitoring.')
			return
		}
		
		// Get status from last execution
		status = new MonitorStatus()

		// Open Chrome driver
		System.setProperty("webdriver.chrome.driver","c:\\chrome-83\\chromedriver.exe");
		ChromeOptions options = new ChromeOptions()
		options.addArguments("start-maximized")
		WebDriver driver = new ChromeDriver(options);
		WebDriverWait wait = new WebDriverWait(driver, waitTimeoutSec);
  	
		//******** Setup - log into ADO ******
		try {
	        // launch browser and direct it to the login page
	        driver.get(msLogin);
			addStep('LOGIN: Launched Login Page')
			
			// Enter userid (email) and click Next
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERID_FIELD))).sendKeys(tfsUser)	
			addStep('LOGIN: Entered userid')
			try {
				wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_BUTTON))).click()
				addStep('LOGIN: Clicked Next')
				// try again in case click did not take
				try {
					driver.findElement(By.id(LOGIN_BUTTON)).click() 
					completedSteps.add('LOGIN: Completed second attempt at clicking on Next')
				} catch(e) {}
			}
			catch (e) { // Next button never became clickable - try re-entering userid
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERID_FIELD))).sendKeys(tfsUser)
				addStep('LOGIN: Completed second attempt at entering userid')
				wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_BUTTON))).click()
				addStep('LOGIN: Completed second attempt at clicking on Next')
			}
			
			// Check for prompt for account type (in case it pops up)
			try {
				WebElement troubleLocatingAccount = driver.findElement(By.id('loginDescription'))
				addStep('LOGIN: Found prompt for account type')
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id('aadTileTitle'))).click()
				addStep('LOGIN: Selected account type')
			} catch (NoSuchElementException e) {}
			
			// Enter password and click Sign in
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id(PASSWORD_FIELD)))
			addStep('LOGIN: Located password entry')
			Thread.sleep(1000) //pause 1 sec
			try {
				driver.findElement(By.id(PASSWORD_FIELD)).sendKeys(tfsPassword)
				addStep('LOGIN: Entered password')
			}
			catch (e) {
				Thread.sleep(5000) //pause 5 sec
				driver.findElement(By.id(PASSWORD_FIELD)).sendKeys(tfsPassword)
				addStep('LOGIN: Entered password a second time after pause')
			}

			// Click Log in
			wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_BUTTON))).click()
			addStep('LOGIN: Clicked Sign in')
			// try one more time in case button did not get clicked
			try {
				driver.findElement(By.id(LOGIN_BUTTON)).click() 
				addStep('LOGIN: Completed second attempt to click Sign in')
			} catch(e) {}
		}
		catch (e) {
			reportError(driver, e,LOGIN_FAILURE)
			CloseBrowser(driver)
			return
		}
		
		// Check ADO availability
		try {
			 // Navigate to ADO collection page
			 driver.get("$tfsUrl/$collection")
			 addStep('ADO VALIDATION: Loading ADO collection page')
			 wait.until(ExpectedConditions.titleIs('Projects - Home'))
		 }
		 catch( e ) {
			 reportError(driver, e,ADO_FAILURE)
			 CloseBrowser(driver)
			 return
		 }

		//********** Begin Modern Requirements Tests *******
		// Test Smart Doc availability
		try {
			// Navigate to Modern Requirements Smart Docs page
			driver.get(mrUrl)
			addStep('SMART DOC VALIDATION: Loading Smart Doc Page')			
			wait.until(ExpectedConditions.titleIs('Smart Docs - Boards'))
			driver.switchTo().frame(0)
			
			// Activate stakeholder license, if the user does not have a permanent account
			if (!hasLicense) {
				String buttonSearchText = "//input[@value=\'Continue as StakeHolder\']"
			    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(buttonSearchText)))  
				addStep('SMART DOC VALIDATION: Waited for Continue as StakeHolder button to be clickable')			
				Thread.sleep(1000) //pause 1 sec
				driver.findElement(By.xpath(buttonSearchText)).click()
				addStep('SMART DOC VALIDATION: clicked on Continue as Stakeholder')
				// try again in case click did not take
				try {
					driver.findElement(By.xpath(buttonSearchText)).click() 
					addStep('SMART DOC VALIDATION: Completed second attempt at clicking Continue as Stakeholder')			
				} catch(e) {}
			}
			
			// Click on the SmartDoc Entry in the tree view
			String smartDocXpath = "//span[contains(.,\'$smartDocName\')]"
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(smartDocXpath)))
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.xpath(smartDocXpath)).click()
			addStep('SMART DOC VALIDATION: clicked on Smart Doc name')
			
			// Check that the root Document work item has rendered in the Smart Doc editor
			if (hasLicense)
				wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ig-smd-grid-wititle-div")))
			else
				wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id=\'smd-main-workitemgrid\']/div[3]/table/tbody/tr/td[3]/div/div[2]")))
			addStep('SMART DOC VALIDATION: validated root work item presence')
			
		}
		catch( e ) {
			reportError(driver, e,SMARTDOC_FAILURE)
			CloseBrowser(driver)
			return
		}
		
		// Test Review Request dialog (must have license to do this)
		if (hasLicense) {
			try {
				// Open the Review Request dialog
				def reviewRequestButton = "#smd-create-review-request > .k-link"
				wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(reviewRequestButton)))
				driver.findElement(By.cssSelector(reviewRequestButton)).click()
				// try again in case click did not take
				try {
					driver.findElement(By.cssSelector(reviewRequestButton)).click()
				} catch(e) {}
				addStep('REVIEW VALIDATION: clicked on Review Request')
				Thread.sleep(5000) //pause 5 sec for dialog to load
				// Check for availability of review title field
				String reviewXpath = "//div[@id=\'phReqReviewTitle\']/div"
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath(reviewXpath)))
				addStep('REVIEW VALIDATION: waited for Review Title to be clickable')
				// Try to click up to 4 times
				multitryClick(driver, 'REVIEW VALIDATION: clicked on Review Title', {By.xpath(reviewXpath)})
				// Close the dialog
				driver.findElement(By.cssSelector(".k-i-rvm-req-dialog-close")).click()
				wait.until(ExpectedConditions.elementToBeClickable(By.id(CONFIRM_BUTTON)))
				Thread.sleep(2000) //pause 2 sec for dialog to load
				// Try to click up to 4 times
				multitryClick(driver, 'REVIEW VALIDATION: clicked on Confirm Button', {By.id(CONFIRM_BUTTON)})
			}
			catch( e ) {
				reportError(driver, e,REVIEW_FAILURE)
				CloseBrowser(driver)
				return
			}
		}
		
		// Success!!!
		completedSteps.each { step -> println(step) }
		log.info("Smart Doc wellness check succeeded.  Elapsed time = $elapsedSec sec")
		
		// If there is a currently failed bug (or previous one), set to SUCCESSFUL RETEST
		resetStatus()
		
		// If system is running too slowly, then report this
//		if (elapsedSec >= tooManySec) reportSlowResponse()
	
		// Log out of both Modern Requirements and ADO
		wait.until(ExpectedConditions.elementToBeClickable(By.id(MR_LOGOUT_BUTTON)))
		driver.findElement(By.id(MR_LOGOUT_BUTTON)).click()
		driver.switchTo().defaultContent()
		wait.until(ExpectedConditions.elementToBeClickable(By.id("mectrl_headerPicture")))
		driver.findElement(By.id("mectrl_headerPicture")).click()
		wait.until(ExpectedConditions.elementToBeClickable(By.id("mectrl_body_signOut")))
		driver.findElement(By.id("mectrl_body_signOut")).click()
		Thread.sleep(10000) //pause 10 sec
		
        //close Browser
        CloseBrowser(driver)

	}
	private void resetStatus() {
		if (status.curBugId) {
			if (updateBugSuccessfulRetest("$status.curBugId")) {
				// Also reset previous open bug
				if (status.prevFail && status.prevFail != '') updateBugSuccessfulRetest("${status.prevFail}")
				// try to delete status file
				if (status.delete()) println('Status file deleted')
			} else {
				log.error("Update to $SUCCESS_TAG failed.  The status file will not be deleted")
			}
		}
	}
	private void multitryClick(WebDriver driver, String stepTitle, Closure by) {
		boolean activated = false
		(0..3).each {
			if (activated) return
			try {
				driver.findElement(by.call()).click()
				addStep(stepTitle)
				activated = true
			} catch(e) {
				addStep("$stepTitle failed - waiting 5 sec before retry")
				Thread.sleep(5000) //pause 5 sec
			}
		}

	}
	private void addStep(stepName) {
		elapsedSec = ((new Date().getTime()) - startTime) / 1000
		completedSteps.add(stepName + " (Elapsed seconds: $elapsedSec)")
	}
	private void reportError(WebDriver driver, Exception e, String newFailType) {
		log.error("$newFailType: ${e.message}")

		// If active bug exists for same failure, just add failure comment.  Else, create new Bug.
		if (status.curBugId && newFailType == status.failType) {
			boolean sentEmail = false
			if ((status.failCount == ticketCount - 1) && newFailType != LOGIN_FAILURE && newFailType != ADO_FAILURE) {
				// Send out email to Modern Requirements Support since it is not a login or ADO failure
				def result
				if (notificationService) result = notificationService.sendModernRequirementsFailureNotification(status)
				if (result == 'success') {
					log.info("Sent support request to $recipientEmailAddresses")	
					sentEmail = true
				}
				else
					log.error("Failed to sent support request to Modern Requirements: $result")	
			}
			updateBugContinuedFailure(status.curBugId, e, newFailType, sentEmail)
		}
		else { // Creating new bug
			String prevFail
			// If previous failure was a login or ADO failure, then tag previous bug as passed
			if (status.curBugId && (status.failType == LOGIN_FAILURE || status.failType == ADO_FAILURE)) {
				if (!updateBugSuccessfulRetest(status.curBugId)) prevFail = status.curBugId
			}
			// If previous failure was a SD page load, and new failure is Review Request failure, then tag previous bug as passed
			else if (status.curBugId && status.failType == SMARTDOC_FAILURE && newFailType == REVIEW_FAILURE) {
				if (!updateBugSuccessfulRetest(status.curBugId)) prevFail = status.curBugId
			}
			else if (status.curBugId)
				prevFail = status.curBugId
			createBug(driver, e, newFailType, prevFail)
		}
	}
	private def createBug(WebDriver driver, Exception e, String newFailType, String prevFail) {
		// Delete cache if it exists
		if (status.curBugId) status.delete()
			
		// Get a screen snaphot and upload to ADO
		File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE)
		String path = "${scrFile.path}"
		String fname = path.substring(path.lastIndexOf('\\')+1,path.length())
		String cachePath = cacheDir + fname
		copyFile(path, cachePath)
		byte[] fileContent = Files.readAllBytes(scrFile.toPath())
		def attData = attachmentService.sendAttachment(fileContent, fname)
		
		// Create a bug in ADO
		println("Creating Bug for: $newFailType")
		def data = []
		def title = "Smart Doc Monitoring $newFailType"
		def desc = "<div>${e.message}" + "<p>" + "${e.cause}".replace('\n','<br>') + '</p></div>' 
		def steps = "<div>" + formatCompletedSteps() + '</div>' 
		data.add([ op: 'add', path: '/id', value: -1])
		data.add([op:'add', path:"/fields/System.Title", value: title])
		data.add([op:'add', path:"/fields/System.Description", value: desc])
		data.add([op:'add', path:"/fields/System.AreaPath", value: areapath])
		data.add([op:'add', path:"/fields/System.AssignedTo", value: owner])
		data.add([op:'add', path:"/fields/Microsoft.VSTS.TCM.ReproSteps", value: steps])
		data.add([op:'add', path:"/fields/System.Tags", value: FAILURE_TAG])
		if (attData) {
			def attUrl = attData.url
			data.add([op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: attUrl, attributes:[comment: 'Selenium Screenshot']]])
		} else {
			log.error("Attachment upload failed: $path")
		}
		def result = workManagementService.createWorkItem(collection, project, 'Bug', data)
		
		// Save status to local file
		if (result) {
			status.curBugId = result.id
			status.failCount = 1
			status.failType = newFailType
			status.title = title
			status.steps = formatCompletedStepsForLog()
			status.error = e.message
			status.cause = e.cause
			status.attName = fname
			status.attPath = cachePath
			status.prevFail = prevFail
			status.save()
			log.info("Created new bug #${status.curBugId}")
		}
		else log.error('Failed to create Bug')
		
	}
	private copyFile(String source, String dest) {
		Path sourceFile = Paths.get(source)
		Path targetFile = Paths.get(dest)
		 
		try {
			Files.copy(sourceFile, targetFile)
		} catch (IOException ex) {
			log.error("I/O Error when copying file $source to $dest");
		}
	}
	private def updateBugSuccessfulRetest(String id) {
		def data = []
		data.add([op:'add', path:"/fields/System.Tags", value: SUCCESS_TAG])
		def result = workManagementService.updateWorkItem(collection, project, id, data)
		if (result) log.info("Updated bug #$id with successful restest")
		return result
	}
	private def updateBugContinuedFailure(String id, Exception e, String failType, boolean emailSent) {
		String comment = "Subsequent test failure: ${e.message}"
		if (emailSent) comment = comment + "<br><br>Support request sent to $recipientEmailAddresses"
		def data = [text: "<div>$comment</div>"]
		def result = workManagementService.addWorkItemComment(collection, project, id, data)
		// Save status to local file
		if (result) {
			status.failCount = status.failCount + 1
			status.save()
			log.info("Updated bug #$id with comment.  Failure count = ${status.failCount}")
		}
		else log.error('Failed to update Bug with comment')
	}
	private String formatCompletedSteps() {
		String html = '<br><p>Completed Steps:<br><ol>'
		completedSteps.forEach { step ->
			html = html + '<li>' + step + '</li>'
		}
		html = html + '</ol></p>'
	}
	private String formatCompletedStepsForLog() {
		String str = 'Completed Steps:\n'
		completedSteps.forEach { step ->
			str = str + '* ' + step + '\n'
		}
		return str
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
	public void CloseBrowser(WebDriver driver)
	{
		driver.close()
		driver.quit()
	}
	private void reportSlowResponse() {
		log.error("WARNING:  Unexpected slow response:\n" + formatCompletedStepsForLog())
	}
	
	// Supporting Class Maintenance Window
	class MaintenanceWindow {
		Date startTime = new Date()
		Date endTime = new Date()
		public MaintenanceWindow(String maintWindow) {
			try {
				startTime.set(hourOfDay: maintWindow.substring(0,2).toInteger(), minute: maintWindow.substring(3,5).toInteger(), second: 0)
				endTime.set(hourOfDay: maintWindow.substring(6,8).toInteger(), minute: maintWindow.substring(9,11).toInteger(), second: 0)
			}
			catch (e) {
				log.error("Invalid maint.window parameter: $maintWindow.  Must be in format HH:MM-HH:MM")
			}
		}
		boolean isActive() {
			if (!startTime || !endTime) return false
			
			Date cur = new Date()
			if (cur >= startTime && cur <= endTime)
				return true
			else
				return false
		}
	}
	
	// Supporting Class that holds persistent monitoring status across invocations
	class MonitorStatus {
		String cachePath
		String curBugId
		int failCount = 0
		String failType
		String title
		String steps
		String error
		String cause
		String attName
		String attPath
		String prevFail
		File attFile
		public MonitorStatus() {
			try {
				cachePath = cacheDir + cacheFilename
				File file = new File(cachePath)
				if (file.exists()) {
					BufferedReader reader = file.newReader()
					def data = reader.text
					reader.close()
					def status = new JsonSlurper().parseText(data)
					this.curBugId = status.id
					this.failCount = status.failCount
					this.failType = status.failType
					this.title = status.title
					this.steps = status.steps
					this.error = status.error
					this.cause = status.cause
					this.attName = status.attName
					this.attPath = status.attPath
					this.prevFail = status.prevFail
					this.attFile = new File(attPath)
				}
			} catch (e) {
				log.error("An error occurred reading cached status: $e.message")
			}
		}
		boolean save() {
			boolean saved = false
			try {
				FileWriter myWriter = new FileWriter(cachePath)
				def output = [id: curBugId, failCount: failCount, failType: failType, title: title, steps: steps, error: error, cause: cause, attName: attName, attPath: attPath, prevFail: prevFail]
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
					if (!attFile.delete()) {
						log.error("Unable to delete attachment file: $attFile.path")
						deleted = false
					}
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
	}
}

