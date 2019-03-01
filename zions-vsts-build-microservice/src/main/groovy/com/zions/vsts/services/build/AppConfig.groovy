package com.zions.vsts.services.build

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient

@Configuration
@ComponentScan("com.zions.vsts.services")
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {
//        @Bean
//        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
//            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
//            propertySourcesPlaceholderConfigurer.setLocations(new ClassPathResource("application.properties"));//or application.yml
//            return propertySourcesPlaceholderConfigurer;
//        }
	@Autowired
	@Value('${cache.location:none}')
	String cacheLocation
	
	@Value('${doc.resource.locations:none}')
	String resourceLocations
	
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		
		registry
		  .addResourceHandler("/doc/**")
		  .addResourceLocations("${resourceLocations}");
	 }
	
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