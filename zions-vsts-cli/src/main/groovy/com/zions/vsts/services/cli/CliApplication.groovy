package com.zions.vsts.services.cli;

import com.zions.vsts.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.service.ProcessTemplateService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class CliApplication implements ApplicationRunner {
	
	@Autowired
	private Map<String, CliAction> actionsMap;
	
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
					action.execute(args);
				} catch (e) {
					log.error(e)
					System.exit(1);
				}
			} else {
				log.error('No action related to command')
				System.exit(1);
			}
		} else {
			log.errorEnabled('No command specified to arguments');
			System.exit(1);
		}
	}
}
