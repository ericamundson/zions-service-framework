package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.WorkItemManagementService
import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

@Component
class CacheWorkitemAttachments implements CliAction {
	AttachmentsManagementService attachmentsManagementService;
	
	WorkItemManagementService workItemManagementService
	
	@Autowired
	public CacheWorkitemAttachments(AttachmentsManagementService attachmentsManagementService,
		WorkItemManagementService workItemManagementService) {
		this.attachmentsManagementService = attachmentsManagementService
		this.workItemManagementService = workItemManagementService
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('ccm.projectArea')[0]
		def workItems = workItemManagementService.getWorkItemsForProject(project)
		while (true) {
			workItems.workItem.each { workitem ->
				int id = Integer.parseInt(workitem.id.text())
				attachmentsManagementService.cacheWorkItemAttachments(id)
			}
			def rel = workItems.@rel
			if ("${rel}" != 'next') break
			workItems = workItemManagementService.nextPage(workItems.@href)
		}
		attachmentsManagementService.rtcRepositoryClient.shutdownPlatform();
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'ccm.projectArea' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
