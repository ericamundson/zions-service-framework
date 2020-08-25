package com.zions.auto.pages

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Value
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.NoSuchElementException
import com.zions.auto.base.BasePage
import com.zions.auto.base.CompletedSteps

import groovy.util.logging.Slf4j

@Component
@Slf4j
class SmartDocsPage extends BasePage {
	@Value('${mr.url}')
	String mrUrl

	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.url}')
	String tfsUrl
	
	@Value('${mr.smartdoc.name:}')
	String smartDocName

	@Value('${mr.haslicense:false}')
	boolean hasLicense

	// UI elements
	static String CONFIRM_BUTTON = "confirm-dialog-ok-button-rvm-req-confirmation-dialog"
	static String MR_LOGOUT_BUTTON = "smd_left_panel_footerbtn-container"
	static String SMARTDOC_NAME_XPATH
	static String SMARTDOC_WI_TITLE_CSS = ".ig-smd-grid-wititle-div"

	boolean go() {
		SMARTDOC_NAME_XPATH = "//span[contains(.,\'$smartDocName\')]"
		
		this.error = null
		try {
			// Navigate to Modern Requirements Smart Docs page
			driver.get(mrUrl)
			steps.add('SMART DOC VALIDATION: Loading Smart Docs Page')			
			wait.until(ExpectedConditions.titleIs('Smart Docs - Boards'))
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("div[class*='external-content'] iframe")))

			// Activate stakeholder license, if the user does not have a permanent account
			if (!hasLicense) {
				String buttonSearchText = "//input[@value=\'Continue as StakeHolder\']"
			    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(buttonSearchText)))  
				steps.add('SMART DOC VALIDATION: Waited for Continue as StakeHolder button to be clickable')			
				Thread.sleep(1000) //pause 1 sec
				driver.findElement(By.xpath(buttonSearchText)).click()
				steps.add('SMART DOC VALIDATION: clicked on Continue as Stakeholder')
				// try again in case click did not take
				try {
					driver.findElement(By.xpath(buttonSearchText)).click() 
					steps.add('SMART DOC VALIDATION: Completed second attempt at clicking Continue as Stakeholder')			
				} catch(e) {}
			}
			
			
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(SMARTDOC_NAME_XPATH)))
			steps.add("SMART DOC VALIDATION: Smart Docs Page was loaded")
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
	
	boolean loadSmartDoc() {
		this.error = null
		try {
			// Click on the SmartDoc Entry in the tree view
			Thread.sleep(1000) //pause 1 sec
			driver.findElement(By.xpath(SMARTDOC_NAME_XPATH)).click()
			steps.add('SMART DOC VALIDATION: clicked on Smart Doc name')
			
			// Check that the root Document work item has rendered in the Smart Doc editor
			if (hasLicense)
				wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SMARTDOC_WI_TITLE_CSS)))
			else
				wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id=\'smd-main-workitemgrid\']/div[3]/table/tbody/tr/td[3]/div/div[2]")))
			steps.add('SMART DOC VALIDATION: validated root work item presence')
			return true
		}
		catch (e) {
			this.error = e
			return false
		}

	}
	
	boolean openReviewRequest() {
		this.error = null
		try {
			// Open the Review Request dialog
			def reviewRequestButton = "#smd-create-review-request > .k-link"
			waitMultiClick({By.cssSelector(reviewRequestButton)}, 'REVIEW VALIDATION: clicked on Review Request',1)

			Thread.sleep(5000) //pause 5 sec for dialog to load
			// Check for availability of review title field
			String reviewXpath = "//div[@id=\'phReqReviewTitle\']/div"
			// Try to click up to 4 times
			waitMultiClick({By.xpath(reviewXpath)}, 'REVIEW VALIDATION: clicked on Review Title',1)
			// Close the dialog
			driver.findElement(By.cssSelector(".k-i-rvm-req-dialog-close")).click()
			steps.add('REVIEW VALIDATION: clicked on Close Button')
			// Try to click up to 4 times
			waitMultiClick({By.id(CONFIRM_BUTTON)}, 'REVIEW VALIDATION: clicked on Confirm Button', 2000)
			return true
		}
		catch( e ) {
			this.error = e
			return false
		}
	}
	
	boolean mrLogOut() {
		this.error = null
		try {
			// Log out of Modern Requirements
			wait.until(ExpectedConditions.elementToBeClickable(By.id(MR_LOGOUT_BUTTON)))
			driver.findElement(By.id(MR_LOGOUT_BUTTON)).click()
			driver.switchTo().defaultContent()
		}
		catch (e) {
			this.error = e
			return false
		}

	}
}
