package com.zions.vsts.services.cli.action.member;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j


@Component
@Slf4j
class GetMembers implements CliAction {
	@Autowired
	MemberManagementService memberManagementService;

	@Value('${tfs.project:}')
	String tfsProject

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		log.info('Getting ADO Project Members...')
		def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
		memberMap.each { member ->
			println(member.value.displayName + ", " + member.value.uniqueName)
			
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url','tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}