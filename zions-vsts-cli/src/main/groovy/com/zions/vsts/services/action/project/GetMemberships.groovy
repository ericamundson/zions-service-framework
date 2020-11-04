package com.zions.vsts.services.action.project;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import com.zions.vsts.services.account.AccountManagementService

/**
 * List out process template data
 *
 * @author Matt
 *
 */
@Component
class GetMemberships implements CliAction {
	
	
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Autowired
	AccountManagementService accountManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.url:}')
	String url
	
	@Value('${tfs.project:}')
	String name
	
	
	
	@Value('${export.dir:}')
	String directory
	
	public GetMemberships() {
	}

	
	
	
	@Override
	public Object execute(ApplicationArguments args) {
		
		def userNameList = []
		//def project = projectManagementService.getProjects(collection)
		//getProject(String collection, String name, boolean noUrl = false)
		def project = projectManagementService.getProject(collection, name)
		def teams = memberManagementService.getAllTeams(collection, project)
		def users = memberManagementService.getProjectMembersMap(collection, name)
		//def getMemberships = memberManagementService.getProjectMembersMap(collection, name)
		users.each { user ->
		String userName = user.value.uniqueName
		userNameList.add(userName)	
		}
	
		println "${userNameList}"

	
		String pName = "${project.name}"
		String id = "${project.id}"
		//def getMemberships = memberManagementService.getProjectMembersMap(collection, pName)

		//println "project: ${pName}"
		//println "projectID: ${id}"
		
		teams.'value'.each { team ->
		String tName = "${team.name}"
		println "project: ${pName}"
		println "team: ${tName}"
		println "user: ${userNameList}"
		}
			
		
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'export.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}
