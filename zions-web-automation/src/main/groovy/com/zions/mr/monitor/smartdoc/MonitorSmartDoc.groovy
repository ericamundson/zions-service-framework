package com.zions.mr.monitor.smartdoc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.vsts.services.work.WorkManagementService
import com.zions.webbot.cli.CliWebBot
import com.zions.vsts.services.notification.NotificationService
import com.zions.auto.base.CompletedSteps
import com.zions.auto.base.Fiddler
import com.zions.auto.pages.CollectionPage
import com.zions.auto.pages.LoginPage
import com.zions.auto.pages.MainHeader
import com.zions.auto.pages.SmartDocsPage
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
class MonitorSmartDoc  implements CliWebBot {
	// Failure types
	String LOGIN_FAILURE = 'ADO login failure'
	String ADO_FAILURE = 'ADO site not available'
	String SMARTDOC_PAGE_FAILURE = 'SD page load failure'
	String SMARTDOC_DOCUMENT_FAILURE = 'SD document load failure'
	String REVIEW_FAILURE = 'Review Request failure'
	
	def failSequence  = [(LOGIN_FAILURE):1,(ADO_FAILURE):2,(SMARTDOC_PAGE_FAILURE):3,(SMARTDOC_DOCUMENT_FAILURE):4,(REVIEW_FAILURE):5]
	
	// Tags
	static String FAILURE_TAG = 'CURRENT OUTAGE'
	static String SUCCESS_TAG = 'SUCCESSFUL RETEST'
	
	MonitorStatus status
	CompletedSteps steps
	
	@Autowired
	Fiddler fiddler

	@Autowired
	WorkManagementService workManagementService

	@Autowired
	IAttachments attachmentService
	
	@Autowired
	NotificationService notificationService
	
	@Autowired
	LoginPage loginPage
	
	@Autowired
	CollectionPage collectionPage	

	@Autowired
	SmartDocsPage smartDocsPage
		
	@Autowired
	MainHeader adoHeader
	
	@Value('${mr.haslicense:false}')
	boolean hasLicense

	@Value('${email.recipient.addresses:}')
	private String[] recipientEmailAddresses

	@Value('${cache.filename:"status.json"}')
	String cacheFilename
	
	@Value('${cache.dir}')
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
	
	@Value('${sel.toomany.sec}')
	int tooManySec
	
	@Value('${ticket.creation.count}')
	int ticketCount

	public boolean checkPreconditions(ApplicationArguments data) {
		// Don't process if during maintenance window
		MaintenanceWindow window = new MaintenanceWindow(maintWindow)
		if ( window.isActive()) {
			log.info('In maintenance window.  No monitoring.')
			return false
		}
		else
			return true
	}
	
	public def execute(ApplicationArguments data, WebDriver driver, WebDriverWait wait, CompletedSteps steps) {
		
		// Get status from last execution
		status = new MonitorStatus()
		
		this.steps = steps

		//******** Log into ADO ******
		if (!loginPage.login()) {
			// try one more time
			steps.add("ERROR: $LOGIN_FAILURE.  Making one more attempt")
			if (!loginPage.login()) {
				reportError(driver, loginPage.error, LOGIN_FAILURE)
				return
			}
		}
		
		// ******** Check ADO availability ********
		if (!collectionPage.go()) {
			// Try one more time
			steps.add("ERROR: $ADO_FAILURE.  Making one more attempt")
			if (!collectionPage.go()) {
				 reportError(driver, collectionPage.error, ADO_FAILURE)
				 return
			}
		 }

		//********** Begin Modern Requirements Tests *******
		// Test Smart Docs landing page availability
		if (!smartDocsPage.go()) {
			steps.add("ERROR: $SMARTDOC_PAGE_FAILURE.  Making one more attempt")
			fiddler.open()
			fiddler.start()
			if (!smartDocsPage.go()) {
				fiddler.stop()
				Thread.sleep(2000)
				fiddler.dump()
				Thread.sleep(2000)
				reportError(driver, smartDocsPage.error, SMARTDOC_PAGE_FAILURE)
				fiddler.close()
				return
			}
			fiddler.close()
		}
		// Test loading of document contents
		if (!smartDocsPage.loadSmartDoc()) {
			steps.add("ERROR: $SMARTDOC_DOCUMENT_FAILURE.  Making one more attempt")
			if (!smartDocsPage.loadSmartDoc()) {
				reportError(driver, smartDocsPage.error, SMARTDOC_DOCUMENT_FAILURE)
				return
			}
		}
		
		// Test Review Request dialog (must have license to do this)
		if (hasLicense) {
			if (!smartDocsPage.openReviewRequest()) {
				reportError(driver, smartDocsPage.error, REVIEW_FAILURE)
				return
			}
		}
		
		// ********** Signout of MR and ADO ***********
		if (!smartDocsPage.mrLogOut()) {
			log.error("WARNING: Error loging out of Modern Requirements: ${smartDocsPage.error}")
		}
		// Log out of ADO
		if (!adoHeader.signout()) 
			log.error("WARNING: Error loging out of ADO: ${adoHeader.error}")
		
		// Success!!!
		log.info("Smart Doc wellness check succeeded.  Elapsed time = ${steps.elapsedSec} sec")
		println(steps.formatForLog())
		
		// If there is a currently failed bug (or previous one), set to SUCCESSFUL RETEST
		resetStatus()
		
		// If system is running too slowly, then report this
//		if (elapsedSec >= tooManySec) reportSlowResponse()
	

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
	private void reportError(WebDriver driver, Exception e, String newFailType) {
		if (!e) {
			log.errorEnabled("Null error for $newFailType")
			return
		}
		log.error("$newFailType: ${e.message}")

		// If active bug exists for same failure, just add failure comment.  Else, create new Bug.
		if (status.curBugId && newFailType == status.failType) {
			boolean sentEmail = false
			if ((status.failCount == ticketCount - 1) && failSequence[newFailType] > failSequence[ADO_FAILURE]) {
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
			// If previous failure was earlier in sequence, then tag previous bug as passed
			if (status.curBugId && failSequence["${status.failType}"] < failSequence[newFailType]) {
				if (!updateBugSuccessfulRetest(status.curBugId)) prevFail = status.curBugId
			}
			// else save in status
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
		def scrAttData = attachmentService.sendAttachment(fileContent, fname)
		
		// Upload Fiddler archive
		def fiddlerAttData
		if (fiddler.hasDump) {
			String path2 = fiddler.fiddlerDump
			File dumpFile = new File(path2)
			String fname2 = path2.substring(path2.lastIndexOf('\\')+1,path2.length())
			byte[] fileContent2 = Files.readAllBytes(dumpFile.toPath())
			fiddlerAttData = attachmentService.sendAttachment(fileContent2, fname2)
		}
		
		// Create a bug in ADO
		println("Creating Bug for: $newFailType")
		def data = []
		def title = "Smart Doc Monitoring $newFailType"
		def desc = "<div>${e.message}" + "<p>" + "${e.cause}".replace('\n','<br>') + '</p></div>' 
		def reproSteps = "<div>" + this.steps.formatForHtml() + '</div>' 
		data.add([ op: 'add', path: '/id', value: -1])
		data.add([op:'add', path:"/fields/System.Title", value: title])
		data.add([op:'add', path:"/fields/System.Description", value: desc])
		data.add([op:'add', path:"/fields/System.AreaPath", value: areapath])
		data.add([op:'add', path:"/fields/System.AssignedTo", value: owner])
		data.add([op:'add', path:"/fields/Microsoft.VSTS.TCM.ReproSteps", value: reproSteps])
		data.add([op:'add', path:"/fields/System.Tags", value: FAILURE_TAG])
		if (scrAttData) {
			def attUrl = scrAttData.url
			data.add([op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: attUrl, attributes:[comment: 'Selenium Screenshot']]])
		} else {
			log.error("Screenshot attachment upload failed: $path")
		}
		if (fiddlerAttData) {
			def attUrl = fiddlerAttData.url
			data.add([op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: attUrl, attributes:[comment: 'Fiddler Archive']]])
		} else if (fiddler.hasDump) {
			log.error("Fiddler attachment upload failed: ${fiddler.fiddlerDump}")
		}
		def result = workManagementService.createWorkItem(collection, project, 'Bug', data)
		
		// Save status to local file
		if (result) {
			status.curBugId = result.id
			status.failCount = 1
			status.failType = newFailType
			status.title = title
			status.steps = this.steps.formatForLog()
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
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	private void reportSlowResponse() {
		log.error("WARNING:  Unexpected slow response:\n" + steps.formatForLog())
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

