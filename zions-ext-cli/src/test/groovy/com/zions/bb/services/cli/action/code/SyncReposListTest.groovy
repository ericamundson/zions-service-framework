package com.zions.bb.services.cli.action.code;

/*import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService

@ContextConfiguration(classes=[SyncReposListTestConfig])
public class SyncReposListTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	SyncReposList underTest
	
	@Autowired(required=true)
	CodeManagementService codeManagmentService
	
	@Autowired(required=true)
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	
	def 'validate ApplicationArguments success flow.'() {
		given: "A stub of RQM get test item request"
		
		//ApplicationArguments args 
		//args = ["tfs.url","tfs.user","tfs.token"] 
		//names.nonOptionArgs('tfs.url', 'tfs.user', 'tfs.token')
		//names = ['tfs.url', 'tfs.user', 'tfs.token',  'in.urls', 'in.user', 'in.password', 'tfs.project', 'tfs.team', 'grant.template', 'repo.dir']
		//ApplicationArguments args = ["tfs.url","tfs.user","tfs.token"] 
		//Set<String> args = new Object[name.length];
		ApplicationArguments args = new File("src/test/resources/application-rm.properties").readLines()
		
		when: 'calling of method under test (validate)'
		def testPlans = underTest.validate(args)
		
		then: ''
		true
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SyncReposListTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	CodeManagementService codeManagmentService() {
		return new CodeManagementService()
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return new MemberManagementService()
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return new EndpointManagementService()
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return new PermissionsManagementService()
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	SyncReposList underTest() {
		return new SyncReposList(CodeManagementService codeManagmentService, 
		PermissionsManagementService permissionsManagementService)
	}

}*/
