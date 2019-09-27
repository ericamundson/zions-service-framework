package com.zions.common.services.user

import static org.junit.Assert.*

import com.zions.common.services.ldap.User
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification


@ContextConfiguration(classes=[UserManagementServiceSpecConfig])
class UserManagementServiceSpec extends Specification {
	@Autowired
	UserManagementService userManagementService

	public 'Find user info'() {
		
		when: w_ 'Access user by id'
		//boolean auth = userManagementService.authenticate('z091182', '4878Middy002')
		User testU = userManagementService.getUserById('z004421')
		
		then: t_ 'validate user found'
		testU != null
		
		when: w_ 'Access user by email'
		User eU = userManagementService.getUserByEmail('Michael.Angelastro@zionsbancorp.com')
		
		then: t_ 'validate user found'
		eU != null

	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.ldap", "com.zions.common.services.user"])
@EnableLdapRepositories(basePackages = "com.zions.common.services.ldap")
@PropertySource("classpath:test.properties")
class UserManagementServiceSpecConfig {
	
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
