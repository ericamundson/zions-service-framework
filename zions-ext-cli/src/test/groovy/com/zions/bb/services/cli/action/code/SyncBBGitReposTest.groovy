package com.zions.bb.services.cli.action.code;

import org.codehaus.groovy.ant.Groovy;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService

import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[SyncBBGitReposTestConfig])
public class SyncBBGitReposTest extends Specification {
	
	@Autowired
	BBCodeManagementService bBCodeManagementService
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	SyncBBGitRepos underTest
	
	def 'execute main flow.'() {
//		given: 'setup of underTest'
//		SyncBBGitRepos underTest = new SyncBBGitRepos(codeManagementService, bBCodeManagementService, permissionsManagementService)
		
		given: 'stub args'
		String[] args = ['--bb.project=stuff', '--tfs.project=stuff', '--bb.password=password',
			'--bb.user=user', '--bb.url=http://', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--grant.template=stuff', '--tfs.team=dumb' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		and: 'stub repos call'
		1 * bBCodeManagementService.getProjectRepoUrls(_) >> [[url: 'stuff', name:'stuff'], [url:'stuff2', name:'stuff2']]
		
		and: 'stub to code management calls'
		2 * codeManagementService.importRepoCLI(_, _, _, _, _, _)
		
		and: 'stub permission calls'
		2 * permissionsManagementService.ensureTeamToRepo(_, _, _, _, _)
		
		when: 'call under test execute'
		boolean success = true
		try {
			underTest.execute(appArgs)
		} catch (e) {
			success = false
		}

		then:
		success
	}
	
	def 'execute flow with delete dir.'() {
//		given: 'setup of underTest'
//		SyncBBGitRepos underTest = new SyncBBGitRepos(codeManagementService, bBCodeManagementService, permissionsManagementService)
		
		given: 'setup git dir'
		File dir = new File('git')
		dir.mkdir()
		
		and: 'stub args'
		String[] args = ['--bb.project=stuff', '--tfs.project=stuff', '--bb.password=password',
			'--bb.user=user', '--bb.url=http://', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--grant.template=stuff', '--tfs.team=dumb' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		and: 'stub repos call'
		1 * bBCodeManagementService.getProjectRepoUrls(_) >> [[url: 'stuff', name:'stuff'], [url:'stuff2', name:'stuff2']]
		
		and: 'stub to code management calls'
		2 * codeManagementService.importRepoCLI(_, _, _, _, _, _)
		
		and: 'stub permission calls'
		2 * permissionsManagementService.ensureTeamToRepo(_, _, _, _, _)
		
		when: 'call under test execute'
		boolean success = true
		try {
			underTest.execute(appArgs)
		} catch (e) {
			success = false
		}

		then:
		success
	}
	def 'validate main flow.'() {
//		given: 'setup of underTest'
//		SyncBBGitRepos underTest = new SyncBBGitRepos(codeManagementService, bBCodeManagementService, permissionsManagementService)
		
		
		given: 'stub args'
		String[] args = ['--bb.project=stuff', '--tfs.project=stuff', '--bb.password=password',
			'--bb.user=user', '--bb.url=http://', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--grant.template=stuff', '--tfs.team=dumb' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		
		
		when: 'call under test execute'
		boolean success = true
		try {
			underTest.validate(appArgs)
		} catch (e) {
			success = false
		}

		then:
		success
	}

}


@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SyncBBGitReposTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(IGenericRestClient)
	}
	
	@Bean
	BBCodeManagementService bBCodeManagementService() {
		return factory.Mock(BBCodeManagementService)
	}
	
	@Bean
	CodeManagementService codeManagementService() {
		return factory.Mock(CodeManagementService)
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return factory.Mock(PermissionsManagementService)
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
	MemberManagementService memberManagementService() {
		return factory.Mock(MemberManagementService)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return factory.Mock(CommandManagementService)
	}
	
	@Autowired
	BBCodeManagementService bBCodeManagementService
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	PermissionsManagementService permissionsManagementService

	@Bean
	SyncBBGitRepos underTest() {
		return new SyncBBGitRepos(codeManagementService, bBCodeManagementService, permissionsManagementService)
	}
		
}

