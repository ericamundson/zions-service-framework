package com.zions.ext.services.cli

import com.mongodb.MongoClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.CheckpointManagementService
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("clmdb")
@ComponentScan(["com.zions.vsts.services","com.zions.clm.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.ldap", "com.zions.common.services.user", "com.zions.common.services.spock.test", "com.zions.common.services.excel"])
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
@EnableLdapRepositories(basePackages = "com.zions.common.services.ldap")
public class CLMDBAppConfig {
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}


	@Bean 
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}
//	@Bean
//	IAttachments attachmentsService() {
//		return new AttachmentManagementService();
//	}
	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		return new MongoClient(dbHost);
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}
	
	//LDAP
	@Value('${ldap.url:}')
	String ldapUrl
	@Value('${ldap.partitionSuffix:}')
	String ldapPartitionSuffix
	@Value('${ldap.principal:}')
	String ldapPrincipal
	@Value('${ldap.password:}')
	String ldapPassword

	@Bean
	public LdapContextSource contextSource() {
		LdapContextSource contextSource = new LdapContextSource();
		 
		contextSource.setUrl(ldapUrl);
		contextSource.setBase(ldapPartitionSuffix);
		contextSource.setUserDn(ldapPrincipal);
		contextSource.setPassword(ldapPassword);
		 
		return contextSource;
	}
	
	@Bean
	public LdapTemplate ldapTemplate() {
		return new LdapTemplate(contextSource());
	}


}