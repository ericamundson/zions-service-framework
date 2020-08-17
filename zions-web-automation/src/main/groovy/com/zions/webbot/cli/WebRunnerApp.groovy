package com.zions.webbot.cli;

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.settings.SettingsManagementService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import com.zions.auto.base.BasePage
import com.zions.auto.base.CompletedSteps

/**
 * Application main for all command-line web automation clients (CliWebBot).  Without mongodb integration.
 * 
 * <p><b>Design:</b></p>
 * <img src="CliApplication.png"/>
 * 
 * @author z097331
 *
 * @startuml
 * class WebRunnerApp [[java:com.zions.webbot.cli.WebRunnerApp]] {
 * 	-Map<String,CliWebBot> actionsMap
 * 	+{static}void main(String[] args)
 * 	+void run(ApplicationArguments args)
 * }
 * interface ApplicationRunner [[java:org.springframework.boot.ApplicationRunner]] {
 * }
 * ApplicationRunner <|.. CliApplication
 * @enduml
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration,LdapAutoConfiguration])
@Slf4j
public class WebRunnerApp implements ApplicationRunner {

	@Autowired
	private Map<String, CliWebBot> actionsMap;
	
	@Autowired(required=false)
	SettingsManagementService settingsManagementService
	
	@Value('${sel.timeout.sec}')
	int waitTimeoutSec

	
	
	/**
	 * 
	 * @param args - command-line arguments
	 */
	static public void main(String[] args) {
		SpringApplication app = new SpringApplication(WebRunnerApp.class);
		app.setBannerMode(Banner.Mode.OFF);
		
		app.run(args);
	}
	
	public void run(ApplicationArguments  args) throws Exception {
		WebDriver driver
		WebDriverWait wait
		def rawArgs = args.sourceArgs
		def command = args.nonOptionArgs
		if (command.size() == 1) {
			CliWebBot action = actionsMap[command[0]]
			if (action != null && action.checkPreconditions(args)) {
				try {
					action.validate(args);
					// Open Chrome driver
					System.setProperty("webdriver.chrome.driver","c:\\chrome-83\\chromedriver.exe");
					ChromeOptions options = new ChromeOptions()
					options.addArguments("enable-automation")
					options.addArguments("--window-size=1920,1080")
					driver = new ChromeDriver(options);
					wait = new WebDriverWait(driver, waitTimeoutSec);
					CompletedSteps steps = new CompletedSteps()
					BasePage.set(driver, wait, steps)
					action.execute(args, driver, wait, steps);
				} catch (e) {
					e.printStackTrace()
					log.error(e)
					System.exit(1);
				} finally {
					// Close browser
//					driver = null // Prevent closing for troubleshooting
					if (driver) {
						driver.close()
						driver.quit()			
						try {
							Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe")
						} catch (IOException e1) {
							e1.printStackTrace()
						}
						log.info("Driver was shut down")
					}
				}
			} else if (action == null){
				log.error('No action related to command')
				System.exit(1);
			}
		} else {
			log.error('No command specified to arguments');
			System.exit(1);
		}
	}
}
