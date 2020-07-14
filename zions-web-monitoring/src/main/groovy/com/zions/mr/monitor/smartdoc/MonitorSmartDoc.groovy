package com.zions.mr.monitor.smartdoc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService

import groovy.util.logging.Slf4j
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.NoSuchElementException
import java.util.concurrent.TimeUnit

@Component
@Slf4j
class MonitorSmartDoc  implements CliAction {
	static String LOGIN_FAILURE = 'ADO login failure'
	static String SMARTDOC_FAILURE = 'Smart Doc failure'
	static String REVIEW_FAILURE = 'Review Request failure'
	
	@Autowired
	WorkManagementService workManagementService

	@Value('${tfs.project:}')
	String project

	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.areapath:}')
	String areapath

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
		String url = data.getOptionValues('mr.url')[0]
        // declaration and instantiation of objects/variables
//    	System.setProperty("webdriver.gecko.driver","C:\\geckodriver.exe");
//		WebDriver driver = new FirefoxDriver();
		//comment the above 2 lines and uncomment below 2 lines to use Chrome
		System.setProperty("webdriver.chrome.driver","c:\\chrome-83\\chromedriver.exe");

		WebDriver driver = new ChromeDriver();
		WebDriverWait wait = new WebDriverWait(driver, waitTimeoutSec);
  	
		//******** Setup - log into ADO ******
		try {
	        // launch browser and direct it to the login page
	        driver.get(msLogin);
			
			// Enter userid (email) and click Next
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("i0116"))).sendKeys(tfsUser)	
			println('Entered userid')
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click()
			// try again in case click did not take
			try {
				driver.findElement(By.id('idSIButton9')).click() 
			} catch(e) {}
			println('Clicked Next')
			
			// Check for prompt for account type (in case it pops up)
			try {
				WebElement troubleLocatingAccount = driver.findElement(By.id('loginDescription'))
				println('Found prompt for account type')
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id('aadTileTitle'))).click()
				println('Selected account type')
			} catch (NoSuchElementException e) {}
			
			// Enter password and click Sing in
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id('i0118')))
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.id('i0118')).sendKeys(tfsPassword)
			println('Entered password')

			// Click Log in
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click()
			// try one more time in case button did not get clicked
			try {
				driver.findElement(By.id('idSIButton9')).click() 
			} catch(e) {}
			println('Clicked Sign in')
		}
		catch (e) {
			log.error("$LOGIN_FAILURE: ${e.message}")
			CloseBrowser(driver)
			createBug(e,LOGIN_FAILURE)
			return
		}
		
		//********** Begin Tests *******
		// Test Smart Doc availability
		try {
			// Navigate to Modern Requirements Smart Docs page
			driver.get(mrUrl)
			println('Loading Smart Doc Page')			
			wait.until(ExpectedConditions.titleIs('Smart Docs - Boards'))
			driver.switchTo().frame(0)
			
			// Activate stakeholder license, if the user does not have a permanent account
			if (!hasLicense) {
				String buttonSearchText = "//input[@value=\'Continue as StakeHolder\']"
			    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(buttonSearchText)))  
				Thread.sleep(1000) //pause 1 sec
				driver.findElement(By.xpath(buttonSearchText)).click()
				// try again in case click did not take
				try {
					driver.findElement(By.xpath(buttonSearchText)).click() 
				} catch(e) {}
				println('clicked on Continue as Stakeholder')
			}
			
			// Click on the SmartDoc Entry in the tree view
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(.,\'$smartDocName\')]"))).click()
			println('clicked on Smart Doc name')
			
			// Check that the root Document work item has rendered in the Smart Doc editor
			if (hasLicense)
				wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ig-smd-grid-wititle-div")))
			else
				wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id=\'smd-main-workitemgrid\']/div[3]/table/tbody/tr/td[3]/div/div[2]")))
			println('validated root work item presence')
			
		}
		catch( e ) {
			log.error("$SMARTDOC_FAILURE: ${e.message}")
			CloseBrowser(driver)
			createBug(e,SMARTDOC_FAILURE)
			return
		}
		
		// Test Review Request dialog (must have license to do this)
		if (hasLicense) {
			try {
				// Open the Review Request dialog
				def reviewRequestButton = "#smd-create-review-request > .k-link"
				driver.findElement(By.cssSelector(reviewRequestButton)).click()
				// try again in case click did not take
				try {
					driver.findElement(By.cssSelector(reviewRequestButton)).click() 
				} catch(e) {}
				println('clicked on Review Request')
				// Check for availability of review title field
				String reviewTitle = "//div[@id=\'phReqReviewTitle\']/div"
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath(reviewTitle)))
				driver.findElement(By.xpath(reviewTitle)).click()
				println('Review Title is available')
			}
			catch( e ) {
				log.error("$REVIEW_FAILURE: ${e.message}")
				CloseBrowser(driver)
				createBug(e,REVIEW_FAILURE)
				return
			}
		}
		
		// Success!!!
		log.info("Smart Doc wellness check succeeded")
	
        //close Browser
        CloseBrowser(driver)

	}
	public void createBug(Exception e, String failureType) {
		println("Creating Bug for: $failureType")
		def data = []
		def title = "Smart Doc Monitoring $failureType"
		def desc = "<div>${e.message}<br><br>" + "${e.cause}".replace('\n','<br>') + '</div>'
		data.add([op:'add', path:"/fields/System.Title", value: title])
		data.add([op:'add', path:"/fields/System.Description", value: desc])
		data.add([op:'add', path:"/fields/System.AreaPath", value: areapath])
		workManagementService.createWorkItem(collection, project, 'Bug', data)
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
		driver.close();
		driver.quit();
	}
}

