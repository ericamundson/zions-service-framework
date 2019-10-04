package com.zions.clm.services.cli.action.work

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.project.planning.PlanManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService

import groovy.json.JsonSlurper

@ContextConfiguration(classes=[ObFilterTestConfig])
public class ObFilterSpecTest extends Specification {
	
	@Value('${test.work.items.file}')
	String testWorkItemsFileName
	
	@Autowired
	ObFilter underTest
	
	@Test
	def 'filter success flow '() {
		given: 'work item query xml'
		def xmlWorkItems = new XmlSlurper().parse(new File(testWorkItemsFileName))
		when: 'call filter'
		def result = underTest.filter(xmlWorkItems)
		then: 'No exception'
		result != null
		println xmlWorkItems
		println '---------------------------------------'
		println result
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ObFilterTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	ObFilter underTest() {
		return new ObFilter()
	}

}
