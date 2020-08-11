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
class MainHeader extends BasePage {

	static String PROFILE_PIC = 'mectrl_headerPicture'
	static String SIGN_OUT = 'mectrl_body_signOut'
	
	public MainHeader() {
	}
	boolean signout() {
		
		error = null
		try {
			driver.switchTo().defaultContent()
			wait.until(ExpectedConditions.elementToBeClickable(By.id(PROFILE_PIC)))
			driver.findElement(By.id(PROFILE_PIC)).click()
			steps.add('SIGNOUT: Clicked profile picture')
			wait.until(ExpectedConditions.elementToBeClickable(By.id(SIGN_OUT)))
			driver.findElement(By.id(SIGN_OUT)).click()
			steps.add('SIGNOUT: Clicked signout link')
			Thread.sleep(10000) //pause 10 sec
			return true
		}
		catch (e) {
			error = e
			return false
		}
	}
}
