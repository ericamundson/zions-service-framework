package com.zions.mr.monitor.smartdoc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService

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
@Component
@Slf4j
class MonitorSmartDoc  implements CliAction {
	static String LOGIN_FAILURE = 'ADO login failure'
	static String SMARTDOC_FAILURE = 'Smart Doc failure'
	static String REVIEW_FAILURE = 'Review Request failure'
	
	def completedSteps = []
	
	@Autowired
	WorkManagementService workManagementService

	@Autowired
	IAttachments attachmentService

	@Value('${cache.file:"c:/lastBugId.txt"}')
	String cacheFile
	
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

	@Value('${tfs.user}')
	String tfsUser

	@Value('${tfs.password}')
	String tfsPassword

	@Value('${sel.timeout.sec}')
	int waitTimeoutSec

	@Value('${mr.haslicense}')
	boolean hasLicense

	public MonitorSmartDoc() {
	}

	public def execute(ApplicationArguments data) {
		/*
		String path = "c:\\screenshot.png"
		File scrFile = new File(path)
		byte[] fileContent = Files.readAllBytes(scrFile.toPath())
		def attData = attachmentService.sendAttachment(fileContent, 'screenshot.png')
		println(attData.url)
		return
		*/
		System.setProperty("webdriver.chrome.driver","c:\\chrome-83\\chromedriver.exe");

		// Open Chrome driver
		ChromeOptions options = new ChromeOptions()
		options.addArguments("start-maximized")
		WebDriver driver = new ChromeDriver(options);
		WebDriverWait wait = new WebDriverWait(driver, waitTimeoutSec);
  	
		//******** Setup - log into ADO ******
		try {
	        // launch browser and direct it to the login page
	        driver.get(msLogin);
			
			// Enter userid (email) and click Next
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("i0116"))).sendKeys(tfsUser)	
			completedSteps.add('LOGIN: Entered userid')
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click()
			completedSteps.add('LOGIN: Clicked Next')
			// try again in case click did not take
			try {
				driver.findElement(By.id('idSIButton9')).click() 
				completedSteps.add('LOGIN: Completed second attempt at clicking on Next')
			} catch(e) {}
			
			// Check for prompt for account type (in case it pops up)
			try {
				WebElement troubleLocatingAccount = driver.findElement(By.id('loginDescription'))
				completedSteps.add('LOGIN: Found prompt for account type')
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id('aadTileTitle'))).click()
				completedSteps.add('LOGIN: Selected account type')
			} catch (NoSuchElementException e) {}
			
			// Enter password and click Sing in
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id('i0118')))
			completedSteps.add('LOGIN: Located password entry')
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.id('i0118')).sendKeys(tfsPassword)
			completedSteps.add('LOGIN: Entered password')

			// Click Log in
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click()
			completedSteps.add('LOGIN: Clicked Sign in')
			// try one more time in case button did not get clicked
			try {
				driver.findElement(By.id('idSIButton9')).click() 
				completedSteps.add('LOGIN: Completed second attempt to click Sign in')
			} catch(e) {}
		}
		catch (e) {
			reportError(driver, e,LOGIN_FAILURE)
			CloseBrowser(driver)
			return
		}
		
		//********** Begin Tests *******
		// Test Smart Doc availability
		try {
			// Navigate to Modern Requirements Smart Docs page
			driver.get(mrUrl)
			completedSteps.add('SMART DOC VALIDATION: Loading Smart Doc Page')			
			wait.until(ExpectedConditions.titleIs('Smart Docs - Boards'))
			driver.switchTo().frame(0)
			
			// Activate stakeholder license, if the user does not have a permanent account
			if (!hasLicense) {
				String buttonSearchText = "//input[@value=\'Continue as StakeHolder\']"
			    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(buttonSearchText)))  
				completedSteps.add('SMART DOC VALIDATION: Waited for Continue as StakeHolder button to be clickable')			
				Thread.sleep(1000) //pause 1 sec
				driver.findElement(By.xpath(buttonSearchText)).click()
				completedSteps.add('SMART DOC VALIDATION: clicked on Continue as Stakeholder')
				// try again in case click did not take
				try {
					driver.findElement(By.xpath(buttonSearchText)).click() 
					completedSteps.add('SMART DOC VALIDATION: Completed second attempt at clicking Continue as Stakeholder')			
				} catch(e) {}
			}
			
			// Click on the SmartDoc Entry in the tree view
			String smartDocXpath = "//span[contains(.,\'$smartDocName\')]"
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(smartDocXpath)))
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.xpath(smartDocXpath)).click()
			completedSteps.add('SMART DOC VALIDATION: clicked on Smart Doc name')
			
			// Check that the root Document work item has rendered in the Smart Doc editor
			if (hasLicense)
				wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ig-smd-grid-wititle-div")))
			else
				wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id=\'smd-main-workitemgrid\']/div[3]/table/tbody/tr/td[3]/div/div[2]")))
			completedSteps.add('SMART DOC VALIDATION: validated root work item presence')
			
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
				completedSteps.add('REVIEW VALIDATION: clicked on Review Request')
				Thread.sleep(5000) //pause 5 sec for dialog to load
				// Check for availability of review title field
				String reviewTitle = "//div[@id=\'phReqReviewTitle\']/div"
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath(reviewTitle)))
				try {
					driver.findElement(By.xpath(reviewTitle)).click()
					completedSteps.add('REVIEW VALIDATION: clicked on Review Title')
				} catch(e) {
					completedSteps.add('REVIEW VALIDATION: clicked on Review Title but unavailable - sleeping 5 sec before trying again')
					Thread.sleep(5000) //pause 5 sec
					// Try again
					driver.findElement(By.xpath(reviewTitle)).click()
					completedSteps.add('REVIEW VALIDATION: clicked on Review Title')
				}
			}
			catch( e ) {
				reportError(driver, e,REVIEW_FAILURE)
				CloseBrowser(driver)
				return
			}
		}
		
		// Success!!!
		completedSteps.each { step -> println(step) }
		String lastBug = getLastBugId()
		if (lastBug && lastBug.length() > 0) {
			updateBugSuccessfulRetest(lastBug)
			deleteCachedBug()
		}
		log.info("Smart Doc wellness check succeeded")
	
        //close Browser
        CloseBrowser(driver)

	}
	private void reportError(WebDriver driver, Exception e, String failureType) {
		log.error("$failureType: ${e.message}")

		// Check to see if a Bug already exists
		String lastBugId = getLastBugId()
		
		if (lastBugId && lastBugId.length() > 0) updateBugContinuedFailure(lastBugId, e, failureType)
		else createBug(driver, e, failureType)
	}
	private def deleteCachedBug() {
		try {
			File file = new File(cacheFile)
			if (file.exists()) file.delete()
			return true
		} catch (IOException e) {
			log.error("An error occurred deleting cached Bug file: $e.message");
			return false
		}

	}
	private def getLastBugId() {
	    try {
			File file = new File(cacheFile)
			if (!file.exists()) return null
			else {
				Scanner myReader = new Scanner(file)
				if (myReader.hasNextLine()) {
				  String bugId = myReader.nextLine()
				  return bugId
				}
				else return null
				myReader.close();
			}
		} catch (IOException e) {
		    log.error("An error occurred reading cached Bug Id: $e.message");
		}
	}
	private def createBug(WebDriver driver, Exception e, String failureType) {

		// Get a screen snaphot and upload to ADO
		File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		String path = "${scrFile.path}"
		log.error("Screenshot at: $path")
		String fname = path.substring(path.lastIndexOf('\\')+1,path.length())
		byte[] fileContent = Files.readAllBytes(scrFile.toPath())
		def attData = attachmentService.sendAttachment(fileContent, fname)
		
		// Create a bug in ADO
		println("Creating Bug for: $failureType")
		def now = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		def data = []
		def title = "Smart Doc Monitoring $failureType"
		def desc = "<div>${e.message}" + "<p>" + "${e.cause}".replace('\n','<br>') + '</p></div>' 
		def steps = "<div>" + formatCompletedSteps() + '</div>' 
		data.add([ op: 'add', path: '/id', value: -1])
		data.add([op:'add', path:"/fields/System.Title", value: title])
		data.add([op:'add', path:"/fields/System.Description", value: desc])
		data.add([op:'add', path:"/fields/System.AreaPath", value: areapath])
		data.add([op:'add', path:"/fields/System.AssignedTo", value: owner])
		data.add([op:'add', path:"/fields/System.CreatedDate", value: now])
		data.add([op:'add', path:"/fields/Microsoft.VSTS.TCM.ReproSteps", value: steps])
		data.add([op:'add', path:"/fields/System.Tags", value: 'CURRENT OUTAGE'])
		if (attData) {
			def attUrl = attData.url
			data.add([op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: attUrl, attributes:[comment: 'Selenium Screenshot']]])
		} else {
			log.error("Attachment upload failed: $path")
		}
		def result = workManagementService.createWorkItem(collection, project, 'Bug', data)
		
		// Save bug number to local file
		if (result) {
			cacheBugId(result.id)
			println("Created new bug")
		}
		else log.error('Failed to create Bug')
		
	}
	private def updateBugSuccessfulRetest(String id) {
		def data = []
		data.add([op:'add', path:"/fields/System.Tags", value: 'SUCCESSFUL RETEST'])
		def result = workManagementService.updateWorkItem(collection, project, id, data)
		if (result) println("Updated bug with successful restest")
	}
	private def updateBugContinuedFailure(String id, Exception e, String failureType) {
		def data = [text:"Subsequent test failure: ${e.message}"]
		def result = workManagementService.addWorkItemComment(collection, project, id, data)
		if (result) println("Updated bug with comment")
	}
	private def cacheBugId(def id) {
		try {
			FileWriter myWriter = new FileWriter(cacheFile)
			myWriter.write("$id")
			myWriter.flush()
			myWriter.close()
		} catch (IOException e) {
			log.error("An error occurred writing to cache file '$cacheFile'.  Error: $e.message");
		}
	}
	private String formatCompletedSteps() {
		String html = '<br><p>Completed Steps:<br><ol>'
		completedSteps.forEach { step ->
			html = html + '<li>' + step + '</li>'
		}
		html = html + '</ol></p>'
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
}

