package com.zions.mr.monitor.smartdoc

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
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
	@Value('${mr.url}')
	String mrUrl

	@Value('${mr.smartdoc.name}')
	String smartDocName

	@Value('${tfs.login}')
	String tfsLogin

	@Value('${tfs.user}')
	String tfsUser

	@Value('${tfs.token}')
	String tfsToken

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
		WebDriverWait wait = new WebDriverWait(driver, 40);
    	
		//******** Setup - log into ADO ******
		try {
	        // launch browser and direct it to the login page
	        driver.get(tfsLogin);
			
			// Enter userid (email) and click Next
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("i0116"))).sendKeys(tfsUser)	
			println('Entered userid')
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click()
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
			driver.findElement(By.id('i0118')).sendKeys(tfsToken)
			println('Entered password')
			// Click Log in
			wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9')))
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.id('idSIButton9')).click() 
			println('Clicked Sign in')
		}
		catch (e) {
			log.error("Failed to log into ADO: ${e.message}")
			driver.close();
			return
		}
		
		//********** Begin Test *******//
		try {
			// Navigate to Modern Requirements Smart Docs page
			driver.get(mrUrl)
			println('Loading Smart Doc Page')			
			wait.until(ExpectedConditions.titleIs('Smart Docs - Boards'))
			println('Title Found')
			
			// Activate stakeholder license
			driver.switchTo().frame(0);
			println('Switched to frame')
		//	driver.findElement(By.cssSelector("html")).click()
		//	println('Clicked on html')
		    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value=\'Continue as StakeHolder\']"))).click()		    
			println('clicked on Continue as Stakeholder')
			
			// Click on the SmartDoc Entry in the tree view
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(.,\'$smartDocName\')]"))).click()
			
			// Check that the root Document work item has rendered in the Smart Doc editor
//			WebElement wiTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ig-smd-grid-wititle-div")))
			WebElement wiTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id=\'smd-main-workitemgrid\']/div[3]/table/tbody/tr/td[3]/div/div[2]")))
		}
		catch( e ) {
			log.error("Smart Doc wellness check failed with error: ${e.message}")
			driver.close()
			return
		}
		
		// Success!!!
		log.info("Smart Doc wellness check succeeded")
		
        //close Browser
        driver.close();
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

