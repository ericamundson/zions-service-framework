package com.zions.vsts.services.admin.project

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementServiceConfig
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ProjectManagementServiceTestConfig])
class ProjectManagementServiceSpecTest extends Specification {
	@Autowired
	ProjectManagementService underTest
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def 'getProject call with project result'() {
		given: 'stub azure devops project request'
		// get stub data from testdata folder
		String json = this.getClass().getResource('/testdata/project.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: 'make call under test'
		def result = underTest.getProject("", "DigitalBanking")
		
		then:
		"${result.id}" == '76e58233-44a1-4241-8d2c-09db3fbca66d'
	}
	
	def 'getProject call with project not found result'() {
		given: 'stub azure devops project request'
		1 * genericRestClient.get(_) >> null
		
		when: 'make call under test'
		def result = underTest.getProject("", "noproject")
		
		then:
		result == null
	}
	
	def 'getProjectProperties with real data result'() {
		given: 'stub azure devops project request'
		1 * genericRestClient.get(_) >> [id: 'umsdflku48', url: 'http://dev.azure.com/uuu/project', name: 'project']
		
		and: 'stub azure devops project properties request'
		//get stub data
		String json = this.getClass().getResource('/testdata/projectproperties.json').text
		JsonSlurper js = new JsonSlurper()
		def result = js.parseText(json)
		1 * genericRestClient.get(_) >> result
		
		when: 'make call under test'
		def r = underTest.getProjectProperties("", 'project')
		
		then:
		"${r.count}" == '8'
	}
	
	def 'getProjectProperty with real data result and return value'() {
		given: 'stub azure devops project request'
		1 * genericRestClient.get(_) >> [id: 'umsdflku48', url: 'http://dev.azure.com/uuu/project', name: 'project']
		
		and: 'stub azure devops project properties request'
		//get stub data
		String json = this.getClass().getResource('/testdata/projectproperties.json').text
		JsonSlurper js = new JsonSlurper()
		def result = js.parseText(json)
		1 * genericRestClient.get(_) >> result
		
		when: 'make call under test'
		String value = underTest.getProjectProperty("", 'project', 'System.CurrentProcessTemplateId')
		
		then:
		"${value}" == '2dc3221a-2d39-4138-a4e1-fc4d20d8912d'
	}
		
	def 'getProjectProperty with real data result'() {
		given: 'stub azure devops project request'
		1 * genericRestClient.get(_) >> [id: 'umsdflku48', url: 'http://dev.azure.com/uuu/project', name: 'project']
		
		and: 'stub azure devops project properties request'
		//get stub data
		String json = this.getClass().getResource('/testdata/projectproperties.json').text
		JsonSlurper js = new JsonSlurper()
		def result = js.parseText(json)
		1 * genericRestClient.get(_) >> result
		
		when: 'make call under test'
		String value = underTest.getProjectProperty("", 'project', 'unknown')
		
		then:
		value == null
	}

	def 'getTeam with real data result'() {
		given: 'stub azure devops team request'
		//get stub data
		String teamjson = this.getClass().getResource('/testdata/projectteam.json').text
		JsonSlurper js = new JsonSlurper()
		def teamresult = js.parseText(teamjson)
		1 * genericRestClient.get(_) >> teamresult
		
		when: 'make call under test'
		def team = underTest.getTeam("", 'DigitalBanking', 'OB')
		
		then:
		"${team.name}" == 'OB'
	}
	
	def 'ensureTeam test with null team return'() {
		given: 'stub azure devops team request and return null for no team'
		//get stub data
		1 * genericRestClient.get(_) >> null
		
		and: 'stub request to create new team'
		String teamjson = this.getClass().getResource('/testdata/projectteam.json').text
		JsonSlurper js = new JsonSlurper()
		def teamresult = js.parseText(teamjson)
		1 * genericRestClient.post(_) >> teamresult
		
		when: 'make call under test'
		def team = underTest.ensureTeam("", 'DigitalBanking', 'OB')
		
		then:
		"${team.name}" == 'OB'
	}
	
	def 'ensureTeam test with good return'() {
		given: 'stub azure devops team request and return a team'
		//get stub data
		String teamjson = this.getClass().getResource('/testdata/projectteam.json').text
		JsonSlurper js = new JsonSlurper()
		def teamresult = js.parseText(teamjson)
		1 * genericRestClient.get(_) >> teamresult
		
		
		when: 'make call under test'
		def team = underTest.ensureTeam("", 'DigitalBanking', 'OB')
		
		then:
		"${team.name}" == 'OB'
	}
	

}

@TestConfiguration
@PropertySource("classpath:test.properties")
class ProjectManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()

	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}

	@Bean
	ProjectManagementService underTest() {
		return new ProjectManagementService();
	}
	
}