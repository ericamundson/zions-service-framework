package com.zions.clm.services.cli.action.member;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.rtc.project.members.CcmMemberManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import groovy.json.JsonSlurper


/**
 * This command-line action ensures CLM users/teams are also up to date with users in ADO
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li> syncProjectWithRTCMembers - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO token</li>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM user</li>
 *  <li>clm.password - CLM password</li>
 *  <li>clm.ccm.project - CLM project name</li>
 *  <li>tfs.project - ADO project name</li>
 *  <li>namemap.json.file - A file to map CLM teams/projects to correct ADO team area/project</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="SyncProjectWithRTCMembers.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class SyncProjectWithRTCMembers {
 * }
 * note left: @Component
 * SyncProjectWithRTCMembers --> com.zions.clm.services.rtc.project.members.CcmMemberManagementService: @Autowired ccmMemberManagmentService - Query the CLM users
 * SyncProjectWithRTCMembers --> com.zions.vsts.services.admin.member.MemberManagementService: @Autowired memberManagmentService - Ensures members/teams in ADO
 * @enduml
 *
 */
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
		def nameMapFileName = data.getOptionValues('namemap.json.file')[0]
		JsonSlurper js = new JsonSlurper()
		def map = js.parse(new File(nameMapFileName))
		memberData = rebuildMemberData(memberData, map)
		memberData.members.each { member ->
			def teams = memberManagmentService.addMemberToTeams(collection, member.id, member.teams)
			
		}
		return null;
	}
	
	def rebuildMemberData(def memberData, map) {
		def nMemberData = [members: []]
		memberData.members.each { member ->
			def nMember = [id: member.id, teams: []]
			member.teams.each { team ->
				map.namemaps.each { amap ->
					if ("${amap.source}" == "${team.team}") {
						String nValue = "${amap.target}"
						def fTeam = nMember.teams.find { iTeam ->
							"${iTeam.team}" == "${nValue}"
						}
						if (fTeam == null) {
							nMember.teams.add([project:team.project, team: nValue])
						}
					}
				}
			}
			nMemberData.members.add(nMember)
		}
		return nMemberData
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'clm.url', 'clm.user', 'clm.password', 'clm.ccm.project', 'tfs.project', 'namemap.json.file']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}