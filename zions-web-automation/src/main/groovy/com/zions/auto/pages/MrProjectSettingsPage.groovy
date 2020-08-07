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
class MrProjectSettingsPage extends BasePage {
	@Value('${mr.url:}')
	String mrUrl
	@Value('${tfs.collection:}')
	String collection
	static String SETTINGS = '__bolt-project-settings-text'
	
	public MrProjectSettingsPage() {
	}
	boolean go(String project) {
		
		this.error = null
		try {
			// launch browser and direct it to the login page
			driver.get("$mrUrl".replace('#project#',project))
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("rightsMngt-breadcrumb")))
			steps.add("MR SETTINGS: Settings Page was loaded for $project")
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
	boolean expandGroup(String group) {
		
		this.error = null
		try {
			driver.switchTo().frame(0)
			// launch browser and direct it to the login page
			WebElement groupEl = driver.findElement(By.xpath("//span[contains(.,\'$group\')]"))
			steps.add("MR SETTINGS: Found group $group")
			groupEl.click()
			Actions action = new Actions(driver)
			action.moveByOffset(-200,0).perform()
			Thread.sleep(10000)
			action.click()
			
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
	boolean enterGroupPermissions(String group) {
		return false
	}
}
