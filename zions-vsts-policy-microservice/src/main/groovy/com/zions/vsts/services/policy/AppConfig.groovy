package com.zions.vsts.services.policy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.CacheManagementService

@Configuration
@ComponentScan("com.zions.vsts.services")
public class AppConfig {
	@Bean 
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
}