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
class CollectionPage extends BasePage {
	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.url}')
	String tfsUrl

	boolean go() {
		
		this.error = null
		try {
			// Navigate to ADO collection page
			driver.get("$tfsUrl/$collection")
			steps.add('ADO VALIDATION: Loading ADO collection page')
			wait.until(ExpectedConditions.titleIs('Projects - Home'))
			steps.add("ADO VALIDATION: ADO collection page was loaded")
			return true
		}
		catch (e) {
			this.error = e
			return false
		}
	}
}
