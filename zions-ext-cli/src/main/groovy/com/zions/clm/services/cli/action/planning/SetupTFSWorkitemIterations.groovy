package com.zions.clm.services.cli.action.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.project.planning.PlanManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService
import groovy.json.JsonBuilder

/**
 * Provides ability to setup ADO sprints from CLM
 * 
 * This is currently not usable. To pull sprints from 
 * ADO required HTML scrapping. New ADO UI broke it.
 * 
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>setupTFSWorkitemIterations - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - </li>
 *  <li>ccm.projectArea - RTC project area</li>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO PAT</li>
 *  <li>tfs.project - ADO project</li>
 *  <li></li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="SetupTFSWorkitemIterations.svg"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class SetupTFSWorkitemIterations [[java:com.zions.clm.services.cli.action.planning.SetupTFSWorkitemIterations]] {
 * 	+SetupTFSWorkitemIterations()
 * 	+def execute(ApplicationArguments data)
 * 	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * CliAction <|.. SetupTFSWorkitemIterations
 * SetupTFSWorkitemIterations --> com.zions.vsts.services.work.planning.IterationManagementService: @Autowired iterationManagementService
 * SetupTFSWorkitemIterations --> com.zions.vsts.services.admin.project.ProjectManagementService: @Autowired projectManagementService
 * SetupTFSWorkitemIterations --> com.zions.clm.services.ccm.project.planning.PlanManagementService: @Autowired planManagementService
 * @enduml
 *
 */
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
		//String clmRootArea = data.getOptionValues('clm.root.area')[0]
		String project = data.getOptionValues('tfs.project')[0]
		def theProject = projectManagementService.getProject(collection, project)
		def iterationData = planManagementService.getIterations(tfsRootArea, projectArea)
		def tfsIterationData = iterationManagementService.getIterationData(collection, project)
		iterationManagementService.processIterationData(collection, theProject, iterationData.iterations, null, tfsIterationData)
		iterationManagementService.ensureTeamsIterations(collection, theProject, tfsIterationData, iterationData.iterations)
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
