package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

@Component
class CacheWorkitemAttachments implements CliAction {
	AttachmentsManagementService attachmentsManagementService;
	
	@Autowired
	public CacheWorkitemAttachments(AttachmentsManagementService attachmentsManagementService) {
		this.attachmentsManagementService = attachmentsManagementService
	}

	public def execute(ApplicationArguments data) {
		String id = data.getOptionValues('workitem.id')[0]
		int aid = Integer.parseInt(id)
		def template = attachmentsManagementService.cacheWorkItemAttachments(aid)
		attachmentsManagementService.rtcRepositoryClient.shutdownPlatform();
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'workitem.id' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
