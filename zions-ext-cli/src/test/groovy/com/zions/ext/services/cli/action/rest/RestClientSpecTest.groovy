package com.zions.ext.services.cli.action.rest

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
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
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService

import groovy.json.JsonSlurper

@ContextConfiguration(classes=[RestClientTestConfig])
public class RestClientSpecTest extends Specification {

	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	Map<String, IGenericRestClient> clientMap

	@Autowired
	RtcRepositoryClient rtcRepositoryClient

	@Autowired
	AreasManagementService areasManagementService;

	@Autowired
	PlanManagementService planManagementService

	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	RestClient underTest

	@Autowired
	CodeManagementService codeManagmentService

	@Autowired
	PermissionsManagementService permissionsManagementService

	@Autowired
	EndpointManagementService endpointManagementService

	@Autowired
	MemberManagementService memberManagementService

	@Autowired
	CommandManagementService commandManagementService

	@Autowired
	CodeManagementService codeManagementService

	@Test
	def 'validate ApplicationArguments success flow.'() {
		given: 'Stub with Application Arguments'
		String[] args = loadArgs('get','json','jsonResponse.json')
		def appArgs = new DefaultApplicationArguments(args)

		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: ''
		result == true
	}

	@Test
	def 'validate ApplicationArguments exception flow.'() {
		given:'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then:
		thrown Exception
	}
	
	@Test
	def 'execute ApplicationArguments success flow for json.' () {
		given:
		given:
		String json = this.getClass().getResource('/testdata/jsonResponse.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out
		genericRestClient.post(_) >> out
		genericRestClient.patch(_) >> out
		genericRestClient.put(_) >> out
		genericRestClient.delete(_) >> out
		genericRestClient.default(_) >> out
		
		when: 'calling of method under test (validate)'
		def resultGet = underTest.execute(new DefaultApplicationArguments(loadArgs('get','json','jsonResponse.json')))
		def resultPost = underTest.execute(new DefaultApplicationArguments(loadArgs('post','json','jsonResponse.json')))
		def resultPatch = underTest.execute(new DefaultApplicationArguments(loadArgs('patch','json','jsonResponse.json')))
		def resultPut = underTest.execute(new DefaultApplicationArguments(loadArgs('put','json','jsonResponse.json')))
		def resultDelete = underTest.execute(new DefaultApplicationArguments(loadArgs('delete','json','jsonResponse.json')))
		def resultDefault = underTest.execute(new DefaultApplicationArguments(loadArgs('default','json','jsonResponse.json')))
		
		then:
		resultGet == null
		resultPost == null
		resultPatch == null
		resultPut == null
		resultDelete == null
		resultDefault == null
	}
	
	@Test
	def 'execute ApplicationArguments success flow for xml.' () {
		given:
		given:
		String json = this.getClass().getResource('/testdata/xmlResponse.xml').text
		JsonSlurper js = new JsonSlurper()
		def out = '<a1>adf</a1>'//js.parseText(json)
		genericRestClient.get(_) >> out
		genericRestClient.post(_) >> out
		genericRestClient.patch(_) >> out
		genericRestClient.put(_) >> out
		genericRestClient.delete(_) >> out
		genericRestClient.default(_) >> out
		
		when: 'calling of method under test (validate)'
		def resultGet = underTest.execute(new DefaultApplicationArguments(loadArgs('get','xml','xmlResponse.xml')))
		def resultPost = underTest.execute(new DefaultApplicationArguments(loadArgs('post','xml','xmlResponse.xml')))
		def resultPatch = underTest.execute(new DefaultApplicationArguments(loadArgs('patch','xml','xmlResponse.xml')))
		def resultPut = underTest.execute(new DefaultApplicationArguments(loadArgs('put','xml','xmlResponse.xml')))
		def resultDelete = underTest.execute(new DefaultApplicationArguments(loadArgs('delete','xml','xmlResponse.xml')))
		def resultDefault = underTest.execute(new DefaultApplicationArguments(loadArgs('default','xml','xmlResponse.xml')))
		
		then:
		resultGet == null
		resultPost == null
		resultPatch == null
		resultPut == null
		resultDelete == null
		resultDefault == null
	}

	private String[] loadArgs(String requestType, String protocol, String responseFile) {
		String[] args = [
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.password=password',
			'--tfs.url=tfsurl',
			'--tfs.user=tfsuser',
			'--tfs.token=tfstoken',
			'--request.file=request.json',
			'--response.file='+responseFile,
			'--request.type='+requestType,
			'--client.name=genericRestClient',
			'--result.protocol='+protocol
		]
		return args
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class RestClientTestConfig {
	def factory = new DetachedMockFactory()

	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(GenericRestClient)
	}
	
	@Bean
	Map<String, IGenericRestClient> clientMap() {
		return ['genericRestClient': genericRestClient()]
	}

	@Bean
	MemberManagementService memberManagementService() {
		return factory.Mock(MemberManagementService)
	}

	@Bean
	AreasManagementService areasManagementService() {
		return factory.Mock(AreasManagementService)
	}

	@Bean
	PlanManagementService planManagementService() {
		return factory.Mock(PlanManagementService)
	}

	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return factory.Mock(RtcRepositoryClient)
	}

	@Bean
	CodeManagementService codeManagementService() {
		return factory.Mock(CodeManagementService)
	}

	@Bean
	ProjectManagementService projectManagementService() {
		return factory.Mock(ProjectManagementService)
	}

	@Bean
	EndpointManagementService endpointManagementService() {
		return factory.Mock(EndpointManagementService)
	}

	@Bean
	PermissionsManagementService permissionsManagementService() {
		return factory.Mock(PermissionsManagementService)
	}

	@Bean
	CommandManagementService commandManagementService() {
		return factory.Mock(CommandManagementService)
	}

	@Bean
	RestClient underTest() {
		return new RestClient()
	}
}
