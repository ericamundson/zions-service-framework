package com.zions.clm.services.cli.action.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.project.planning.PlanManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService
import groovy.json.JsonBuilder

@Component
class SetupTFSWorkitemIterations implements CliAction {
	@Autowired
	IterationManagementService iterationManagementService;
	
	@Autowired
	PlanManagementService planManagementService
	
	@Autowired
	ProjectManagementService projectManagementService

	public SetupTFSWorkitemIterations() {
	}

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String projectArea = data.getOptionValues('ccm.projectArea')[0]
		String tfsRootArea = data.getOptionValues('tfs.root.area')[0]
		String project = data.getOptionValues('tfs.project')[0]
		def theProject = projectManagementService.getProject(collection, project)
		def iterationData = planManagementService.getIterations(tfsRootArea, projectArea)
		def tfsIterationData = iterationManagementService.getIterationData(collection, project)
		iterationManagementService.processIterationData(collection, theProject, iterationData.iterations, null, tfsIterationData)
		//areasManagementService.assignTeamAreas(collection, theProject, areaData.teams)
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'ccm.projectArea', 'tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'tfs.root.area' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
