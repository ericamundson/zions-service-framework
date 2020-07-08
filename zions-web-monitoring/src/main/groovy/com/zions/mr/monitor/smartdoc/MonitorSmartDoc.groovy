package com.zions.mr.monitor.smartdoc

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
import org.openqa.selenium.NoSuchElementException
import java.util.concurrent.TimeUnit

@Component
@Slf4j
class MonitorSmartDoc  implements CliAction {
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
    	
        String baseUrl = 'https://login.microsoftonline.com/'
        String expectedTitle = "Smart Docs - Boards";
        String actualTitle = "";

        // launch Fire fox and direct it to the Base URL
        driver.get(baseUrl);
		WebDriverWait wait = new WebDriverWait(driver, 20);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("i0116"))).sendKeys('robert.huet@zionsbancorp.com')	
		wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click() // Click Next

		// Check for prompt for account type
		try {
			WebElement troubleLocatingAccount = driver.findElement(By.id('loginDescription'))
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id('aadTileTitle'))).click()
			wait(driver,2)
		} catch (NoSuchElementException e) {
		}
		
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id('i0118'))).sendKeys('augus1L!berty')
		wait.until(ExpectedConditions.elementToBeClickable(By.id('idSIButton9'))).click() // Click Log in

		driver.get('https://dev.azure.com/ZionsETO/DTS/_apps/hub/edevtech-mr.iGVSO-OnPrem-mrserviceus1008.subHubWork-SmartDocs-OnPrem#teamId=ab30c6e9-39b1-47bf-a42b-b0c63c48a54b')

        // get the actual value of the title
		wait.until(ExpectedConditions.titleIs(expectedTitle))
        actualTitle = driver.getTitle();

        /*
         * compare the actual title of the page with the expected one and print
         * the result as "Passed" or "Failed"
         */
        if (actualTitle.contentEquals(expectedTitle)){
            println("Test Passed!");
        } else {
            println("Test Failed");
        }
		WebElement smartDocLink = driver.findElement(By.xpath("//*[text()='TestDoc']"))
		
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
	private void wait(WebDriver driver, int sec) {
		driver.manage().timeouts().implicitlyWait(sec, TimeUnit.SECONDS)
	}

}

