package com.zions.bb.services.cli.action.code;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper


/**
 * Command line class to import GIT repos from Bitbucket project to VSTS project and apply grants for access to
 * repos to specific team.
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li> syncBBGitRepos - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>tfs.url - ADO host url</li>
 *  <li>tfs.user - ADO user id</li>
 *  <li>(optional) tfs.token - ADO PAT</li>
 *  <li>bb.url - Bitbucket URL</li>
 *  <li>bb.user - Bitbucket user</li>
 *  <li>bb.password - Bitbucket password</li>
 *  <li>bb.project - Butbucket Project</li>
 *  <li>tfs.project - ADO project</li>
 *  <li>tfs.team - ADO Team</li>
 *  <li>grant.template - general permissions to set on repo for team</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="SyncBBGitRepos.svg"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class SyncBBGitRepos [[java:com.zions.bb.services.cli.action.code.SyncBBGitRepos]] {
 * 	~CodeManagementService codeManagmentService
 * 	~BBCodeManagementService bBCodeManagmentService
 * 	+SyncBBGitRepos(CodeManagementService codeManagmentService, BBCodeManagementService bBCodeManagmentService, PermissionsManagementService permissionsManagementService)
 * 	+def execute(ApplicationArguments data)
 * 	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * CliAction <|.. SyncBBGitRepos
 * SyncBBGitRepos --> com.zions.vsts.services.code.CodeManagementService: @Autowired codeManagmentService
 * SyncBBGitRepos --> com.zions.bb.services.code.BBCodeManagementService: @Autowired bBCodeManagmentService
 * @enduml
 */
@Component
class SyncBBGitRepos implements CliAction {
	
	@Autowired
	CodeManagementService codeManagmentService
	
	@Autowired
	BBCodeManagementService bBCodeManagmentService
	
	@Autowired
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	public SyncBBGitRepos(CodeManagementService codeManagmentService, 
		BBCodeManagementService bBCodeManagmentService,
		PermissionsManagementService permissionsManagementService) {
		this.codeManagmentService = codeManagmentService
		this.bBCodeManagmentService = bBCodeManagmentService;
		this.permissionsManagementService = permissionsManagementService
	}
	/**
	 *  Execute command line.
	 *  
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 */
	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('bb.project')[0]
		String outproject = data.getOptionValues('tfs.project')[0]
		String bbUser = data.getOptionValues('bb.user')[0]
		String bbPassword = data.getOptionValues('bb.password')[0]
		String teamName = data.getOptionValues('tfs.team')[0]
		String templateName = data.getOptionValues('grant.template')[0]
		def repos = bBCodeManagmentService.getProjectRepoUrls(project)
		repos.each { repo ->
			def url = repo.url.replace("${bbUser}@", '')
			codeManagmentService.importRepoCLI(collection, outproject, repo.name, url, bbUser, bbPassword)	
			permissionsManagementService.ensureTeamToRepo(collection, outproject, repo.name, teamName, templateName)		
		}
		File git = new File('git')
		if (git.exists()) {
			git.deleteDir()
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#validate(org.springframework.boot.ApplicationArguments)
	 */
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'bb.url', 'bb.user', 'bb.password', 'bb.project', 'tfs.project', 'tfs.team', 'grant.template']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}