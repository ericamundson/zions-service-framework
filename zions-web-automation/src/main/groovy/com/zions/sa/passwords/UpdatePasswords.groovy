package com.zions.sa.passwords

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.notification.NotificationService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.webbot.cli.CliWebBot
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.auto.base.BasePage
import com.zions.auto.base.CompletedSteps
import com.zions.auto.pages.LoginPage
import com.zions.auto.pages.MainHeader
import com.zions.auto.pages.MrProjectSettingsPage
import com.zions.auto.pages.SetPasswordPage
import groovy.util.logging.Slf4j
import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.NoSuchElementException
import java.util.concurrent.TimeUnit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

@Component
@Slf4j
class UpdatePasswords  implements CliWebBot {
	
	@Autowired
	SetPasswordPage setPasswordPage
	
	@Autowired
	LoginPage loginPage
	
	@Value('${tfs.collection:}')
	String collection
	
	String email
	String oldpw
	String newpw
	
	public boolean checkPreconditions(ApplicationArguments data) {
		
		
		// Get all projects for Org
		def projects = ["abc", "bcd"]
		if (!projects) {
			log.error("Could not retrieve projects for: $collection")
			return false
		}
		
		if (projects.size() == 0) {
			log.info("No more projects to process.  To process all projects, delete the history")
			return false
		}
		else
			return true
	
	}
	
	public def execute(ApplicationArguments data, WebDriver driver, WebDriverWait wait, CompletedSteps steps) {
	
		// Get all service account usernames
		//def saccounts = ["svc-cloud2-adomaint001@zionsbancorporation.onmicrosoft.com", "svc-cloud-adomaint002@zionsbancorporation.onmicrosoft.com"]
		def saccounts = ["joe@msn.com", "pete@msn.com"]
		println(saccounts)
		def saOldPasswords = ["oldpw", "oldpw2"]
		def saNewPasswords = ["newpw", "newpw2"]

		//******** for each service account access the password change page ******
		for(int i=0; i<saccounts.size(); i++) {
			def numAccountsToProcess = saccounts.size()
			//def count = 0
			email = saccounts[i]
			oldpw = saOldPasswords[i]
			newpw = saNewPasswords[i]
			//def test = "abc"
			
			try {
				log.info("There are $numAccountsToProcess accounts to process")
				if (!setPasswordPage) {
					log.error("Could not load password settings page for user $saccounts[i]")
					return
				}
				
				log.info("*** Processing password updates for user: $saccounts[i]")
				def updatePw = setPasswordPage.setNewPassword(email, oldpw, newpw, newpw)
				
				if (updatePw) {
					log.info("password updated for user $email")
					
				}
				
				else {
					log.info("password update FAILED for user $email")
				}
				
			}
			catch (e) {
				log.error("Aborting: ${e.message}")
				log.error(steps.formatForLog())
				return
			}
			
						
		}

			
		// Success!!!
		println("all records processed")
		
	}
	private void reportError(WebDriver driver, Exception e, String newFailType) {
		log.error("$newFailType: ${e.message}")

	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['pw.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}

