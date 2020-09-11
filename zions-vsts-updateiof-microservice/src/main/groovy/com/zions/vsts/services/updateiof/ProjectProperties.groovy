package com.zions.vsts.services.updateiof

import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@Component
@ConfigurationProperties(prefix = "config")
@EnableConfigurationProperties
public class ProjectProperties
{
    List<Project> projects = new ArrayList<Project>();

    // Getter/Setter for gateways
    // ...

    public static class Project
    {
        String name;
		String[] types;
		String[] states;



        // Getters and Setters
        // ...
    }
}