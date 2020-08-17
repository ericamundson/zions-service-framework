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
import org.openqa.selenium.Keys
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
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
	
	boolean go(String project) {
		
		this.error = null
		try {
			// launch browser and direct it to the login page
			driver.get("$mrUrl".replace('#project#',project))
//			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("rightsMngt-breadcrumb")))
			steps.add("MR SETTINGS: Loading Settings Page for $project")
			driver.switchTo().frame(0)
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ig-TreeGridControl-rightMngt-treeGrid-container")))
			steps.add("MR SETTINGS: Settings Page was loaded")
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}

	private boolean scrollDownToFindAndClick(Closure findElementBy) {
//		JavascriptExecutor js = (JavascriptExecutor) driver
//		js.executeScript("window.scrollTo(0,0)")
		Actions actionObject = new Actions(driver)
		WebElement element
		int maxFailCount = 300
		int count
		boolean found = false
		while (count <= maxFailCount && !found) {
			try {
				element = driver.findElement(findElementBy.call())

				if (multiClick(element, "MR SETTINGS: click group")) {
					found = true
					if (count > 0) {
						Thread.sleep(500);
						((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element)
						Thread.sleep(500);
					}
				}
				else 
					count = maxFailCount + 1
			} 
			catch (NoSuchElementException e) 
			{
				found = false
				actionObject = actionObject.sendKeys(Keys.ARROW_DOWN)
				actionObject.perform()
				Thread.sleep(200)
				count++
				println("count=$count")
				this.error = e
			}
			catch (other) 
			{
				found = false
				count = maxFailCount + 1
				this.error = other
			}
		}
		return found
		
	}
	boolean enterGroupPermissions(String group, def settings) {
		String[] permissions = new String[8]
		int i = 0
		settings.each { setting ->
			if (setting == 'Allow')
				permissions[i++] = ''
			else
				permissions[i++] = '[2]'
		}
		
		this.error = null
		int ms = 200
		int count = 0
		try {
			String GROUP_LINK = "//span[text()='$group']"
			if (!scrollDownToFindAndClick({By.xpath(GROUP_LINK)})) {
				log.error("Failed to locate $group.  Error: ${this.error}")
				return false
			}
			steps.add("MR SETTINGS: Selected group $group")
			
			wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Common Settings")))
			steps.add("MR Settings: Settings displayed for $group")
			Thread.sleep(1000)
			
			// Create/EditFolder
			waitMultiClick({By.xpath("//span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[14]/div/div[2]/ul/li${permissions[0]}")},"MR SETTINGS: Click permission$count")
			
			// Delete Folder
			waitMultiClick({By.xpath("//div[2]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[15]/div/div[2]/ul/li${permissions[1]}")},"MR SETTINGS: Click permission$count")
			
			// Create/Update Artifact
			waitMultiClick({By.xpath("//div[3]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[16]/div/div[2]/ul/li${permissions[2]}")},"MR SETTINGS: Click permission$count")
			
			// Delete Artifact
			waitMultiClick({By.xpath("//div[4]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[17]/div/div[2]/ul/li${permissions[3]}")},"MR SETTINGS: Click permission$count")
			
			// Save As Template
			waitMultiClick({By.xpath("//div[5]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[18]/div/div[2]/ul/li${permissions[4]}")},"MR SETTINGS: Click permission$count")
			
			// Smart Report Generation
			waitMultiClick({By.xpath("//div[6]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[19]/div/div[2]/ul/li${permissions[5]}")},"MR SETTINGS: Click permission$count")
			
			// Smart Report Designer
			waitMultiClick({By.xpath("//div[7]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[20]/div/div[2]/ul/li${permissions[6]}")},"MR SETTINGS: Click permission$count")
			
			// Create/Update Meta Template
			waitMultiClick({By.xpath("//div[2]/div/div[5]/div/div/div[2]/span/span/span/span")},"MR SETTINGS: Click dropdown${++count}")
			Thread.sleep(ms)
			waitMultiClick({By.xpath("//div[25]/div/div[2]/ul/li${permissions[7]}")},"MR SETTINGS: Click permission$count")

			steps.add("MR SETTINGS: Permissions set for $group")
			
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
	boolean clickGroup(String group) {
		String GROUP_LINK = "//span[text()='$group']"
		try {
			click({By.xpath(GROUP_LINK)})
			return true
		}
		catch (e) {
			return false
		}
		
	}
}
