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
import org.openqa.selenium.JavascriptExecutor;

import com.zions.auto.base.CompletedSteps

import groovy.util.logging.Slf4j

@Component
@Slf4j
class BasePage {	
	static WebDriver driver
	static WebDriverWait wait
	static CompletedSteps steps
	Exception error
	private Integer zoomValue = 100
	private Integer zoomIncrement = 20

	public static void set(WebDriver driver, WebDriverWait wait, CompletedSteps steps) {
		this.driver = driver
		this.wait = wait
		this.steps = steps
	}
	
	protected void click(Closure by) {
		wait.until(ExpectedConditions.elementToBeClickable(by.call()))
		driver.findElement(by.call()).click()
	}
	protected void waitMultiClick(Closure by, def stepTitle = null, int pauseMs = 0) {
		if (pauseMs) {
			wait.until(ExpectedConditions.elementToBeClickable(by.call()))
			Thread.sleep(pauseMs)
		}
		boolean activated = false
		(0..6).each {
			if (activated) return
			try {
				driver.findElement(by.call()).click()
				if (stepTitle) steps.add(stepTitle)
				activated = true
			} catch(e) {
				if (stepTitle) steps.add("$stepTitle failed - waiting 5 sec before retry")
				Thread.sleep(5000) //pause 5 sec
			}
		}

	}
	protected boolean multiClick(WebElement element, def stepTitle = null) {
		boolean activated = false
		(0..6).each {
			if (activated) return
			try {
				element.click()
				if (stepTitle) steps.add(stepTitle)
				activated = true
			} catch(e) {
				if (stepTitle) steps.add("$stepTitle failed - waiting 5 sec before retry")
				Thread.sleep(5000) //pause 5 sec
			}
		}
		return activated
	}
	public void zoomIn() {
		zoomValue += zoomIncrement;
		zoom(zoomValue);
	}
	public void zoomOut() {
		zoomValue -= zoomIncrement;
		zoom(zoomValue);
	}
	private void zoom(def level) {
		JavascriptExecutor js = (JavascriptExecutor) driver
		js.executeScript("window.scrollTo(0,0)");
		
		String cmd = "document.body.style.zoom=’" + level + "%’"
//		js.executeScript("document.body.style.zoom = '1.5'")
//		js.executeScript(cmd);
	}
}
