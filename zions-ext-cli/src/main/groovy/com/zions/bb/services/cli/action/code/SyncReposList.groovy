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
 * Command line class to import GIT repos from a directory of repositories to VSTS project and apply grants for access to
 * repos to specific team.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li> syncReposList - The action's Spring bean name.</li>
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
 * @author z091182
 *
 */
@Component
class SyncReposList implements CliAction {
	
	@Autowired
	CodeManagementService codeManagmentService
	
	@Autowired
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	public SyncReposList(CodeManagementService codeManagmentService, 
		PermissionsManagementService permissionsManagementService) {
		this.codeManagmentService = codeManagmentService
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
		String outproject = data.getOptionValues('tfs.project')[0]
		String inUser = data.getOptionValues('in.user')[0]
		String inPassword = data.getOptionValues('in.password')[0]
		String teamName = data.getOptionValues('tfs.team')[0]
		String repoDirName = data.getOptionValues('repo.dir')[0]
		String templateName = data.getOptionValues('grant.template')[0]
		File repoDir = new File(repoDirName)
		repoDir.listFiles().each { cRepoDir ->
			codeManagmentService.importRepoDir(collection, outproject, cRepoDir.name, cRepoDir, inUser, inPassword)	
			permissionsManagementService.ensureTeamToRepo(collection, outproject, cRepoDir.name, teamName, templateName)		
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
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'in.urls', 'in.user', 'in.password', 'tfs.project', 'tfs.team', 'grant.template', 'repo.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}