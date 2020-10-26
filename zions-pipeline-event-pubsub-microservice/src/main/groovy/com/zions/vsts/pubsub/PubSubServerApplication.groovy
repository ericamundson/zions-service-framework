package com.zions.vsts.pubsub;


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
//import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration


/**
 * WebSocket micro-service main class.
 * 
 * <img src="ms-component-diagram.svg"/>
 * @author z091182
 * 
 * @startuml ms-component-diagram.svg
 * cloud AzureDevOps as "Azure DevOps" {
 * 	  component ADOPipelineTask as "Pipeline Task"
 * }
 * 
 * cloud ZionsOrAzure as "Rancher Kubernetes docker" {
 *    component PipelineEventPubSubMicroService as "[[https://dev.azure.com/ZionsETO/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-event-pubsub-microservice%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fvsts%2Fpubsub%2FEventController.groovy zions-pipeline-event-pubsub-microservice]] provides pub-sub of internal pipeline events."
 *    ADOPipelineTask --> PipelineEventPubSubMicroService: "Provides single endpoint for all pipeline command action event registrations."
 *   component CommandExecutionMicroService as "Command Execution Micro Service"
 *   
 *   CommandExecutionMicroService --> PipelineEventPubSubMicroService: "Subscribe to pipeline actions to execute ADO/XLD/XLR behaviors"
 *   
 * }
 *
 * 
 * @enduml
 *
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration,LdapAutoConfiguration])
public class PubSubServerApplication {


	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(PubSubServerApplication.class);
		app.run(args);
				

	}
}
