package com.zions.vsts.services.cli.action.member;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import groovy.json.JsonSlurper


@Component
class AddMembers implements CliAction {
	MemberManagementService memberManagmentService
	
	@Autowired
	public AddMembers(MemberManagementService memberManagmentService) {
		this.memberManagmentService = memberManagmentService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String inFile = data.getOptionValues('member.file.name')[0]
		File mFile = new File(inFile)
		JsonSlurper s = new JsonSlurper()
		def memberData = s.parseText(mFile.text)
		memberData.members.each { member ->
			def teams = memberManagmentService.addMember(collection, member.id, member.teams)
			
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token',  'member.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}