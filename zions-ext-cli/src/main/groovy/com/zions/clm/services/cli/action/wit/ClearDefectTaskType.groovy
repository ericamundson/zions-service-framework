package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

@Component
class ClearDefectTaskType implements CliAction {
	FixWorkItemIssuesService fixWorkItemIssuesService;
	
	@Autowired
	public ClearDefectTaskType(FixWorkItemIssuesService fixWorkItemIssuesService) {
		this.fixWorkItemIssuesService = fixWorkItemIssuesService
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('clm.project')[0]
		def template = fixWorkItemIssuesService.clearTaskTypeOnDefect(project)
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.project' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
