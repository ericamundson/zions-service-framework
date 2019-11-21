package com.zions.clm.services.ccm.workitem

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.test.SpockLabeler

import spock.lang.Specification

@ContextConfiguration(classes=[CcmWorkManagementServiceSpecTestConfig])
class CcmWorkManagementServiceSpecTest extends Specification {

	public void 'Initial test'() {
		//fail("Not yet implemented")
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.clm.services","com.zions.clm.services.ccm", "com.zions.common.services.test.generators"])
@PropertySource("classpath:test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class CcmWorkManagementServiceSpecTestConfig {
	
}