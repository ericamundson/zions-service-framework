package com.zions.vsts.services.cli.action.wit;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.user.services.MemberManagementService
import com.zions.vsts.services.work.templates.service.ProcessTemplateService

@Component
class AddMember implements CliAction {
	MemberManagementService memberManagmentService
	Map actionsMap
	
	@Autowired
	public AddMember(Map actionsMap, MemberManagementService memberManagmentService) {
		this.actionsMap = actionsMap;
		this.memberManagmentService = memberManagmentService
	}

	@Override
	public def execute(ApplicationArguments data) {
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token', 'tfs.project', 'tfs.workitem.name','in.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}