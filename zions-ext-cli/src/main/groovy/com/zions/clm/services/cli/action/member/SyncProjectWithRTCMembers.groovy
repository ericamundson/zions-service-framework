package com.zions.clm.services.cli.action.member;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.rtc.project.members.CcmMemberManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import groovy.json.JsonSlurper


@Component
class SyncProjectWithRTCMembers implements CliAction {
	MemberManagementService memberManagmentService
	CcmMemberManagementService ccmMemberManagmentService
	
	@Autowired
	public SyncProjectWithRTCMembers(MemberManagementService memberManagmentService, CcmMemberManagementService ccmMemberManagmentService) {
		this.memberManagmentService = memberManagmentService
		this.ccmMemberManagmentService = ccmMemberManagmentService;
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('clm.ccm.project')[0]
		String outproject = data.getOptionValues('tfs.project')[0]
		def memberData = ccmMemberManagmentService.getMemberData(project, outproject)
		memberData.members.each { member ->
			def teams = memberManagmentService.addMember(collection, member.id, member.teams)
			
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'clm.url', 'clm.user', 'clm.password', 'clm.ccm.project', 'tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}