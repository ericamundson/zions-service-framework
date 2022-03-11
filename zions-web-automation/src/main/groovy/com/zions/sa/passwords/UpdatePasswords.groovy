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
	
	@Value('${pw.url:}')
	String pwurl
	
	@Value('${tfs.sausers:}')
	String[] saccounts
	
	@Value('${tfs.oldpws:}')
	String[] saOldPasswords
	
	@Value('${tfs.newpws:}')
	String[] saNewPasswords
	
	String email
	String oldpw
	String newpw

	
	public boolean checkPreconditions(ApplicationArguments data) {
		

		
		if (!saccounts || !saOldPasswords || !saNewPasswords) {
			log.error("please load user login old password and new password in properties file")
			return false
		}

		else
			return true
	
	}
	
	public def execute(ApplicationArguments data, WebDriver driver, WebDriverWait wait, CompletedSteps steps) {
	
		//Input usernames and passwords in the pwapplication.properties file before running.

		//******** for each service account access the password change page ******
		for(int i=0; i<saccounts.size(); i++) {
			def numAccountsToProcess = saccounts.size()
			
			email = saccounts[i]
			oldpw = saOldPasswords[i]
			newpw = saNewPasswords[i]

			//update the passwords			
			try {
				
				if (!setPasswordPage) {
					log.error("Could not load password settings page for user $saccounts[i]")
					return
				}
				
				int count = i + 1
				log.info("*** Updating password for $count of $numAccountsToProcess accounts for user: $email")
				def updatePw = setPasswordPage.setNewPassword(email, oldpw, newpw, newpw)
				
				if (updatePw) {
					log.info("password updated for user $email")
					
				}
				
				else {
					log.info("password update FAILED for user $email")
				}
				
			}
			catch (e) {
				log.error("Unable to update password for the user $email: ${e.message} moving to next user")

			}
			
						
		}

			
		// Success!!!
		println("all records processed")
		
	}
	private void reportError(WebDriver driver, Exception e, String newFailType) {
		log.error("$newFailType: ${e.message}")

	}
	
	public Object validate(ApplicationArguments args) throws Exception {


		return true
	}

}

