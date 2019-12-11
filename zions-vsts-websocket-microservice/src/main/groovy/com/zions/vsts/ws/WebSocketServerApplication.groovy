package com.zions.vsts.ws;


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
import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration


/**
 * WebSocket micro-service main class.
 * 
 * @author z091182
 * 
 * @startuml ms-component-diagram.png
 * cloud AzureDevOps as "Azure DevOps" {
 * 	  component WebHook as "Web Hook"
 * }
 * cloud AzureCloud as "Azure Kubernetes Docker" {
 *    component ZionsWSMicroService as "[[https://dev.azure.com/ZionsETO/DTS/_git/zions-service-framework?path=%2Fzions-vsts-websocket-microservice&version=GBmaster zions-ado-websocket-microservice]] provides pub-sub of ADO events."
 *    WebHook --> ZionsWSMicroService: "Provides single endpoint for all ADO event registrations."
 * }
 * 
 * cloud ZionsOrAzure as "Zions or Azure Kubernetes docker" {
 *   component WorkRollupMS as "[[https://dev.azure.com/ZionsETO/DTS/_git/zions-service-framework?path=%2Fzions-vsts-rollup-microservice&version=GBmaster ALMOps Work Rollup micro-service]]"
 *   component PolicyMS as "[[https://dev.azure.com/ZionsETO/DTS/_git/zions-service-framework?path=%2Fzions-vsts-policy-microservice&version=GBmaster Release Engineering GIT Policy micro-service]]"
 *   
 *   WorkRollupMS --> ZionsWSMicroService: "Subscribe to work item change events to handle any work effort rollup."
 *   PolicyMS --> ZionsWSMicroService: "Subscribe to GIT change events that require policy enforcement."
 *   
 *   WorkRollupMS --> AzureDevOps: "Communicate Feature level effort."
 *   PolicyMS --> AzureDevOps: "Communicate any GIT policy changes
 * }
 *
 * 
 * @enduml
 *
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration,LdapAutoConfiguration,LdapDataAutoConfiguration])
public class WebSocketServerApplication {


	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(WebSocketServerApplication.class);
		app.run(args);
				

	}
}
