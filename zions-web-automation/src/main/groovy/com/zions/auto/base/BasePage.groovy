package com.zions.auto.base

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

import com.zions.auto.base.CompletedSteps

import groovy.util.logging.Slf4j

@Component
@Slf4j
class BasePage {	
	WebDriver driver
	WebDriverWait wait
	CompletedSteps steps
	Exception error
	
	public void set(WebDriver driver, WebDriverWait wait, CompletedSteps steps) {
		this.driver = driver
		this.wait = wait
		this.steps = steps
	}
	
	public void click(Closure by) {
		wait.until(ExpectedConditions.elementToBeClickable(by.call()))
		driver.findElement(by.call()).click()
	}
}
