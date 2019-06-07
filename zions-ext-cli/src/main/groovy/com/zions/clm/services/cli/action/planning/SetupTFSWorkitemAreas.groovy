package com.zions.clm.services.cli.action.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.project.planning.PlanManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.workitem.AreasManagementService
import groovy.json.JsonBuilder

/**
 * Provides ability to setup ADO team areas from CLM
 * 
 * This is currently not usable. To pull team areas from 
 * TFS required HTML scrapping. New ADO UI broke it.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>setupTFSWorkitemAreas - The action's Spring bean name.</li>
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
 * <img src="SetupTFSWorkitemAreas.png"/>
 * 
 * @author z091182
 *
 * @startuml
 * class SetupTFSWorkitemAreas [[java:com.zions.clm.services.cli.action.planning.SetupTFSWorkitemAreas]] {
 * 	~AreasManagementService areasManagementService
 * 	~ProjectManagementService projectManagementService
 * 	+SetupTFSWorkitemAreas()
 * 	+def execute(ApplicationArguments data)
 * 	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * CliAction <|.. SetupTFSWorkitemAreas
 * SetupTFSWorkitemAreas --> AreasManagementService: @Autowired areasManagementService
 * SetupTFSWorkitemAreas --> PlanManagementService:  @Autowired planManagementService
 * SetupTFSWorkitemAreas --> ProjectManagementService: @Autowired projectManagementService
 * @enduml
 */
@Component
class SetupTFSWorkitemAreas implements CliAction {
	@Autowired
	AreasManagementService areasManagementService;
	
	@Autowired
	PlanManagementService planManagementService
	
	@Autowired
	ProjectManagementService projectManagementService

	public SetupTFSWorkitemAreas() {
	}

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String projectArea = data.getOptionValues('ccm.projectArea')[0]
		String tfsRootArea = ""
		try {
			tfsRootArea = data.getOptionValues('tfs.root.area')[0]
		} catch (err) {}
		String project = data.getOptionValues('tfs.project')[0]
		def theProject = projectManagementService.getProject(collection, project)
		def areaData = planManagementService.getCategories(tfsRootArea, projectArea)
		def tfsAreaData = areasManagementService.getAreaData(collection, project)
		areasManagementService.processAreasData(collection, theProject, areaData.areas, null, tfsAreaData)
		areasManagementService.assignTeamAreas(collection, theProject, areaData.teams)
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'ccm.projectArea', 'tfs.url', 'tfs.user', 'tfs.token', 'tfs.project' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
