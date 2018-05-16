package com.zions.vsts.services.admin.rest

import com.zions.common.services.model.response.Status
import com.zions.vsts.services.admin.user.services.MemberManagementService

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod

@RestController()
class MemberEndPoint {
	
	@Autowired
	MemberManagementService memberManagementService
	
	@RequestMapping(value = "/addMembers", method = RequestMethod.POST)
	public ResponseEntity addMembers(@RequestBody String json) {
		JsonSlurper slurper = new JsonSlurper()
		def memberData = slurper.parseText(json)
		memberData.members.each { member ->
			def projects = []
			member.projects.each { project ->
				projects.add(project)
			}
			memberManagementService.addMember(member.id, member.role, projects)(buildData)
		}
		return ResponseEntity.ok(HttpStatus.OK)
	}

}
