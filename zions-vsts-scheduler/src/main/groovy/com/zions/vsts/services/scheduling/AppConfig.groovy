package com.zions.vsts.services.scheduling

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
@Profile("default")
@ComponentScan(["com.zions.vsts.services"])
public class AppConfig implements SchedulingConfigurer {
    private final int POOL_SIZE = 10;

    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix("my-scheduled-task-pool-");
        threadPoolTaskScheduler.initialize();

        scheduledTaskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }

	@Autowired
	@Value('${cache.location:cache:cache}')
	String cacheLocation

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
	
	@Bean 
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
}