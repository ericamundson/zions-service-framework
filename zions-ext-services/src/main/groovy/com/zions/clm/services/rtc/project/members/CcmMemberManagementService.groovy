package com.zions.clm.services.rtc.project.members

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder

/**
 * Enables extracting member data from CLM module projects.
 * 
 * @author z091182
 *
 */
@Component
public class CcmMemberManagementService {
	@Autowired(required=true)
	private IGenericRestClient clmGenericRestClient;
	
	@Autowired
	@Value('${clm.context}')
	String clmContext

	
	public CcmMemberManagementService() {
		
	}
	
	/**
	 * Entry point for extracting CLM project member data
	 * @param project - clm project.
	 * @param tfsproject - ADO project name to relate member data
	 * @return data structure of member data
	 */
	public def getMemberData(String project, String tfsproject) {
		def query = "foundation/projectArea[name='${project}']/(name|teamMembers/userId|teamMembers/emailAddress|teamMembers/archived|allTeamAreas/archived|allTeamAreas/name|allTeamAreas/teamMembers/userId|allTeamAreas/teamMembers/emailAddress|allTeamAreas/teamMembers/archived)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/${clmContext}/rpt/repository/foundation?fields=" + encoded;
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
				def id = member.emailAddress.text()
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
				team = team.replace(' ', '')
				teamArea.teamMembers.each { member ->
					if ("${member.archived.text()}" == 'false') {
						def id = member.emailAddress.text()
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
		//println json
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
