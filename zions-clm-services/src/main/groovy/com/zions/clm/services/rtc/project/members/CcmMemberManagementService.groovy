package com.zions.clm.services.rtc.project.members

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.clm.services.rest.ClmGenericRestClient
import groovy.json.JsonBuilder

@Component
public class CcmMemberManagementService {
	@Autowired(required=true)
	private ClmGenericRestClient clmGenericRestClient;
	
	public CcmMemberManagementService() {
		
	}
	
	public def getMemberData(String project, String tfsproject) {
		def query = "foundation/projectArea[name='${project}']/(name|teamMembers/userId|teamMembers/archived|allTeamAreas/archived|allTeamAreas/name|allTeamAreas/teamMembers/userId|allTeamAreas/teamMembers/archived)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/foundation?fields=" + encoded;
		def result = clmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'text/xml'] );
		return buildMemberData(result, tfsproject);
	}
	
	private def buildMemberData(def teams, def tfsproject) {
		def memberMap = [:]
		def defaultTeam = "${tfsproject} Team"
		teams.projectArea.teamMembers.each { member ->
			if ("${member.archived.text()}" == 'false') {
				def id = member.userId.text()
				if (!memberMap.containsKey(id)) {
					memberMap[id] = [[project: tfsproject, team: defaultTeam]]
				} else {
					memberMap[id].add([project: tfsproject, team: defaultTeam])
				}
			}
		}
		teams.projectArea.allTeamAreas.each { teamArea ->
			if ("${teamArea.archived.text()}" == 'false') {
				def team = teamArea.name.text()
				teamArea.teamMembers.each { member ->
					if ("${member.archived.text()}" == 'false') {
						def id = member.userId.text()
						if (!memberMap.containsKey(id)) {
							memberMap[id] = [[project: tfsproject, team: team]]
						} else {
							memberMap[id].add([project: tfsproject, team: team])
						}
					}
				}
			}
		}
		def out = []
		memberMap.each { key, val ->
			out.add([id: key, teams: val])
		}
		def json = new JsonBuilder(['members': out]).toPrettyString()
		println json
		return ['members': out]
	}

	private nextPage(String url) {
		def result = clmGenericRestClient.get(
				uri: url,
				headers: [Accept: 'text/xml']
				)
		return result
	}
}
