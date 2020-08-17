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
		try {
			click({By.xpath("//span[text()='$group']")})
			steps.add("MR SETTINGS: Selected group $group")
			Thread.sleep(1000)
			// Create/EditFolder
			driver.findElement(By.xpath("//span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[14]/div/div[2]/ul/li${permissions[0]}")).click()
			
			// Delete Folder
			driver.findElement(By.xpath("//div[2]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[15]/div/div[2]/ul/li${permissions[1]}")).click()
			
			// Create/Update Artifact
			driver.findElement(By.xpath("//div[3]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[16]/div/div[2]/ul/li${permissions[2]}")).click()
			
			// Delete Artifact
			driver.findElement(By.xpath("//div[4]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[17]/div/div[2]/ul/li${permissions[3]}")).click()
			
			// Save As Template
			driver.findElement(By.xpath("//div[5]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[18]/div/div[2]/ul/li${permissions[4]}")).click();
			
			// Smart Report Generation
			driver.findElement(By.xpath("//div[6]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[19]/div/div[2]/ul/li${permissions[5]}")).click()
			
			// Smart Report Designer
			driver.findElement(By.xpath("//div[7]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[20]/div/div[2]/ul/li${permissions[6]}")).click()
			
			// Create/Update Meta Template
			driver.findElement(By.xpath("//div[2]/div/div[5]/div/div/div[2]/span/span/span/span")).click()
			Thread.sleep(ms)
			driver.findElement(By.xpath("//div[25]/div/div[2]/ul/li${permissions[7]}")).click()
		
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
}
