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
 * List out project and team data.
 * 
 * @author Astro
 *
 */
@Component
class getProcessTemplateData implements CliAction {
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	@Value('${export.dir:}')
	String directory
	
	public getProcessTemplateData() {
	}

	

	@Override
	public Object execute(ApplicationArguments args) {
	
		def projects = projectManagementService.getProjects(collection)
	
		//getProjects(collection, project, noUrl)
		
		projects.'value'.each { project ->
			//
			String pName = "${project.name}"
			String id = "${project.id}"
		
		
		
		def templates = processTemplateService.getWorkitemTemplateFields(collection, pName, workItemName)
		
		templates.each { template -> 
			println template.displayName
			println template.mailAddress
		}
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
