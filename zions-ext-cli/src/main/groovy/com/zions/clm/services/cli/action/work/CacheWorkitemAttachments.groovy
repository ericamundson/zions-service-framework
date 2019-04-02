package com.zions.clm.services.cli.action.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

/**
 * Currently not used.  Training sample for Astro.
 * 
 * @author z091182
 *
 */
@Component
class CacheWorkitemAttachments implements CliAction {
	AttachmentsManagementService attachmentsManagementService;
	
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Autowired
	public CacheWorkitemAttachments(AttachmentsManagementService attachmentsManagementService,
		ClmWorkItemManagementService workItemManagementService) {
		this.attachmentsManagementService = attachmentsManagementService
		this.clmWorkItemManagementService = workItemManagementService
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('ccm.projectArea')[0]
		def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(project)
		while (true) {
			workItems.workItem.each { workitem ->
				int id = Integer.parseInt(workitem.id.text())
				attachmentsManagementService.cacheWorkItemAttachments(id)
			}
			def rel = workItems.@rel
			if ("${rel}" != 'next') break
			workItems = clmWorkItemManagementService.nextPage(workItems.@href)
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
