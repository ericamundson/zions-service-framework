package com.zions.vsts.services.cli


import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import com.zions.vsts.services.work.templates.ProcessTemplateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.test.TestManagementService

@Configuration
@Profile("project")
@ComponentScan(["com.zions.vsts.services.action.project,com.zions.vsts.services.asset,com.zions.vsts.services.work,com.zions.vsts.services.action.test,com.zions.vsts.services.admin,com.zions.vsts.services.test"])

public class ProjectAppConfig {
    Map<String, CliAction> actions;
    
    @Bean
    public Map<String, CliAction> actionsMap() {
        if (actions == null)
            actions = [:];
        return actions;
    }
	
	
	@Bean
	ProcessTemplateService processTemplateService() {
		return new ProcessTemplateService();
	}
	
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService();
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return new MemberManagementService();
	}


    @Autowired
    @Value('${cache.location:cache}')
    String cacheLocation
    
    @Value('${tfs.url:}')
    String tfsUrl
    @Value('${tfs.user:}')
    String tfsUser
    @Value('${tfs.token:}')
    String tfsToken


  
    @Bean 
    ICacheManagementService cacheManagementService() {
        return new CacheManagementService(cacheLocation)
    }
	
    
    @Bean
    CommandManagementService commandManagementService() {
        return new CommandManagementService();
    }
    
    @Bean
    IGenericRestClient genericRestClient() {
        return new MultiUserGenericRestClient()
    }
    
   
}
