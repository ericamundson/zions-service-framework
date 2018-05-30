package com.zions.vsts.services.build

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import com.zions.common.services.cli.action.CliAction

@Configuration
@ComponentScan("com.zions.vsts.services")
public class AppConfig {
        @Bean
        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            propertySourcesPlaceholderConfigurer.setLocations(new ClassPathResource("application.properties"));//or application.yml
            return propertySourcesPlaceholderConfigurer;
        }
}