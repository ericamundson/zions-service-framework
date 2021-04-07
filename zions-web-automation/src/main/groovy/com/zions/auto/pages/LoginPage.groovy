package com.zions.auto.pages

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.NoSuchElementException
import com.zions.auto.base.BasePage
import com.zions.auto.base.CompletedSteps

import groovy.util.logging.Slf4j

@Component
@Slf4j
class LoginPage extends BasePage {
	@Value('${tfs.url}')
	String tfsUrl
	
	@Value('${ms.login}')
	String msLogin
	
	@Value('${ms.officeHomeTitle}')
	String msOfficeTitle

	@Value('${tfs.user}')
	String tfsUser
	
	@Value('${tfs.password}')
	String tfsPassword

	static String LOGIN_BUTTON = 'idSIButton9'
	static String PASSWORD_FIELD = 'i0118'
	static String USERID_FIELD = 'i0116'
	
	public LoginPage() {
	}
	boolean login() {
		
		error = null
		try {
			// launch browser and direct it to the login page
			driver.get(msLogin);
			steps.add('LOGIN: Launched Login Page')
			
			// Enter userid (email) and click Next
			try {
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERID_FIELD))).sendKeys(tfsUser)
				steps.add('LOGIN: Entered userid')
			}
			catch (e) { // Check to see if user is already logged in (seen this condition in Bug 1711427)
				if (ExpectedConditions.titleIs(msOfficeTitle)) {
					steps.add('LOGIN: User already logged in')
					return true;
				}
				else {
					error = e
					return false
				}
			}
			try {
				wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_BUTTON))).click()
				steps.add('LOGIN: Clicked Next')
				// try again in case click did not take
				try {
					driver.findElement(By.id(LOGIN_BUTTON)).click()
					steps.add('LOGIN: Completed second attempt at clicking on Next')
				} catch(e) {}
			}
			catch (e) { // Next button never became clickable - try re-entering userid
				wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERID_FIELD))).sendKeys(tfsUser)
				steps.add('LOGIN: Completed second attempt at entering userid')
				wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_BUTTON))).click()
				steps.add('LOGIN: Completed second attempt at clicking on Next')
			}
			
			// Check for prompt for account type (in case it pops up)
			try {
				WebElement troubleLocatingAccount = driver.findElement(By.id('loginDescription'))
				steps.add('LOGIN: Found prompt for account type')
				waitMultiClick({By.id('aadTileTitle')},'LOGIN: Selected account type',50)
			} catch (NoSuchElementException e) {}
			
			// Enter password and click Sign in
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id(PASSWORD_FIELD)))
			steps.add('LOGIN: Located password entry')
			Thread.sleep(1000) //pause 1 sec
			try {
				driver.findElement(By.id(PASSWORD_FIELD)).sendKeys(tfsPassword)
				steps.add('LOGIN: Entered password')
			}
			catch (e) {
				Thread.sleep(5000) //pause 5 sec
				driver.findElement(By.id(PASSWORD_FIELD)).sendKeys(tfsPassword)
				steps.add('LOGIN: Entered password a second time after pause')
			}

			// Click Log in
			waitMultiClick({By.id(LOGIN_BUTTON)}, 'LOGIN: Clicked Sign in',1)	
			Thread.sleep(1000) //pause 1 sec, not sure this is needed, but Bug 1711427 indicates this might help
			wait.until(ExpectedConditions.titleIs(msOfficeTitle))
			
			return true
		}
		catch (e) {
			error = e
			return false
		}
	}
}
