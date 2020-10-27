package com.zions.vsts.services.action.project;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

/**
 * List out process template data
 * 
 * @author Matt
 *
 */
@Component
class GetProcessTemplateData implements CliAction {
	
	
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.url:}')
	String url
	
	
	@Value('${export.dir:}')
	String directory
	
	public GetProcessTemplateData() {
	}

	

	@Override
	public Object execute(ApplicationArguments args) {
	
		def projects = projectManagementService.getProjects(collection)
		
		
		//first get work item types
		//def getWITField(collection, project, wit, field)
		//def getFields(def collection, def project)
		
		projects.'value'.each { project -> 
//			
			String pName = "${project.name}"
			String id = "${project.id}"
			println "project: ${pName}"
			println "projectID: ${id}"
		
		//def getProjectMembersMap(collection, project)
		def getMemberships = memberManagementService.getProjectMembersMap(collection, pName)
		/*getMemberships.each { member ->
			
				def identity = member.identity
				String uid = "${identity.uniqueName}"
				println uid
		}*/
		return null
	}
	
		
		def users = memberManagementService.getUsers(collection)
				
		users.each { user -> 
			println user.displayName
			println user.mailAddress
		}
		return null
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
