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
class SetPasswordPage extends BasePage {
	
	@Value('${pw.url:}')
	String pwUrl
		
	static String emailBox = 'userNameInput'
	static String oldpwBox = 'oldPasswordInput'
	static String newpwBox = 'newPasswordInput'
	static String confpwBox = 'confirmNewPasswordInput'
	static String submitButton = 'submitButton'
	static String cancelButtom = 'cancelButton'

	
	def setNewPassword(String email, String oldpw, String newpw, String confpw) {
		driver.get("$pwUrl")
		driver.switchTo().defaultContent()

		driver.findElement(By.id(emailBox)).click()
		driver.findElement(By.id(emailBox)).sendKeys(email)
		
		driver.switchTo().defaultContent()
		driver.findElement(By.id(oldpwBox)).click()
		driver.findElement(By.id(oldpwBox)).sendKeys(oldpw)

		
		driver.switchTo().defaultContent()
		driver.findElement(By.id(newpwBox)).click()
		driver.findElement(By.id(newpwBox)).sendKeys(newpw)
		
		driver.switchTo().defaultContent()
		driver.findElement(By.id(confpwBox)).click()
		driver.findElement(By.id(confpwBox)).sendKeys(newpw)
		
		//wait.until(ExpectedConditions.elementToBeClickable(By.id(submitButton)))
		driver.findElement(By.id(submitButton)).click()
		//If successfully updated the following message will appear:
		/*Your password is successfully updated*/
		def message = driver.findElement(By.id("expiredNotification")).getText() == "Your password is successfully updated."
		
		if (message) {
			return true
		}
		else {
			return false
		}
		
		Thread.sleep(10000) //pause 10 sec
				
	}
}