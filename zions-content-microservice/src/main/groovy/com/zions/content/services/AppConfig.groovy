package com.zions.content.services


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter



/* Will set default configs for ContentApplication */

@Configuration
@ComponentScan("com.zions.content.services")
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {

	@Value('${doc.resource.locations:none}')
	String resourceLocations

	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		registry
				.addResourceHandler("/doc/**")
				.addResourceLocations("${resourceLocations}");

		/* registry.addResourceHandler("/doc/**")
		 .addResourceLocations("file:///C:/Resources/");*/
	}
}


