package com.zions.vsts.services.mapfield

import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@Component
@ConfigurationProperties(prefix = "config")
@EnableConfigurationProperties
public class ProjectProperties
{
	List<String> inputFields = new ArrayList<String>();
	String outputField;
	String mapname;
	
	
    // Getter/Setter for gateways
    // ...


	

	
}