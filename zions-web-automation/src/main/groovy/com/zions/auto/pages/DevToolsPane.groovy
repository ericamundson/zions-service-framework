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
class DevToolsPane extends BasePage {

	static String PROFILE_PIC = 'mectrl_headerPicture'
	static String SIGN_OUT = 'mectrl_body_signOut'
	
	boolean open() {
		
		error = null
		try {
			return true
		}
		catch (e) {
			error = e
			return false
		}
	}
	boolean openNetworkTab() {
		
		error = null
		try {
			return true
		}
		catch (e) {
			error = e
			return false
		}
	}
}
