package com.zions.common.services.logging

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.TriggerContext
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

import groovy.util.logging.Slf4j

import java.time.Instant
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Provides interval heartbeat logging for any service.  To use it on any service, just add
 * this class to the services AppConfig class as an "extends". 
 * 
 * The interval is configured in minutes.
 * 
 * @author Robert Huet
 *
 */
@EnableScheduling
@Slf4j
public class HeartbeatLoggingConfig implements SchedulingConfigurer {
	@Value('${spring.application.name}')
	String appName
	
	@Value('${heartbeat.minutes:25}')
	String springHeartbeatMinutes
	
	int heartbeatMinutes
	
	// Heartbeat Implementation
	@Bean
	public Executor taskExecutor() {
		return Executors.newSingleThreadScheduledExecutor();
	}
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskExecutor());
		taskRegistrar.addTriggerTask(
		  new Runnable() {
			  @Override
			  public void run() {
				  if (heartbeatMinutes > 0)
					  log.info("$appName heartbeat")
			  }
		  },
		  new Trigger() {
			  @Override
			  public Date nextExecutionTime(TriggerContext context) {
				  // Get interval minutes from Environment Var (injected from K8s ConfigMap) or default to spring boot config
				  if (heartbeatMinutes == 0) {
					  heartbeatMinutes = springHeartbeatMinutes.toInteger()
					  if (heartbeatMinutes > 0)
						  log.info("Heartbeat logging set for every $heartbeatMinutes minutes")
				  }
				  
				  Instant nextExecutionTime = new Date().toInstant()
					  .plusMillis(heartbeatMinutes * 60000)
				  return Date.from(nextExecutionTime)
			  }

			@Override
			public Instant nextExecution(TriggerContext triggerContext) {
				// TODO Auto-generated method stub
				return null;
			}
		  }
		)
	}

}
