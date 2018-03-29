package com.zions.vsts.services.work.templates

import com.zions.vsts.services.work.templates.service.ProcessTemplateService
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
class TemplateManagementApplication implements ApplicationRunner {
	
	@Autowired
	private ProcessTemplateService processTemplateService;
	
	static public void main(String[] args) {
		SpringApplication app = new SpringApplication(TemplateManagementApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
		
		

	}
	
	public void run(ApplicationArguments  args) throws Exception {
		
	}
}
