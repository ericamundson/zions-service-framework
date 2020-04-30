package com.zions.ext.services.cli;

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.settings.SettingsManagementService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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

/**
 * Application main for all command-line actions.  Without mongodb integration.
 * 
 * <p><b>Design:</b></p>
 * <img src="CliApplication.png"/>
 * 
 * @author z091182
 *
 * @startuml
 * class CliApplication [[java:com.zions.ext.services.cli.CliApplication]] {
 * 	-Map<String,CliAction> actionsMap
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
public class CliApplication implements ApplicationRunner {
	@Autowired
	private Map<String, CliAction> actionsMap;
	
	@Autowired(required=false)
	SettingsManagementService settingsManagementService
	
	
	/**
	 * 
	 * @param args - command-line arguments
	 */
	static public void main(String[] args) {
		SpringApplication app = new SpringApplication(CliApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		
		app.run(args);
	}
	
	public void run(ApplicationArguments  args) throws Exception {
		def rawArgs = args.sourceArgs
		def command = args.nonOptionArgs
		if (command.size() == 1) {
			CliAction action = actionsMap[command[0]]
			if (action != null) {
				try {
					action.validate(args);
//					if (settingsManagementService) {
//						settingsManagementService.turnOffNotifications('')
//					}
					action.execute(args);
				} catch (e) {
					e.printStackTrace()
					log.error(e)
					System.exit(1);
				} finally {
//					if (settingsManagementService) {
//						settingsManagementService.turnOnNotifications('')
//					}
				}
			} else {
				log.error('No action related to command')
				System.exit(1);
			}
		} else {
			log.error('No command specified to arguments');
			System.exit(1);
		}
	}
}
