package com.zions.vsts.services.account

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.tfs.rest.GenericRestClient
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[AccountManagementServiceSpecConfig])
class AccountManagementServiceSpec extends Specification implements SpockLabeler {

	@Autowired
	AccountManagementService underTest

	@Ignore	
	def 'get all accounts'() {
		when: w_ 'run getAccounts'
		def accounts = underTest.getAccounts('')
		
		then: t_ 'return size is > 0'
		accounts.value.size() > 0
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class AccountManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.url}')
	String tfsUrl
	
	@Value('${tfs.user}')
	String tfsUser
	
	@Value('${tfs.token}')
	String tfsToken
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new GenericRestClient(tfsUrl, tfsUser, tfsToken)
	}
	
	@Bean
	AccountManagementService underTest() {
		return new AccountManagementService()
	}

}
