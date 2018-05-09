package com.zions.vsts.services.build.rest

import com.zions.vsts.services.build.service.BuildService
import com.zions.common.services.model.response.Status
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod

@RestController()
class BuildEndPoint {
	
	@Autowired
	BuildService buildService
	
	@RequestMapping(value = "/tag", method = RequestMethod.POST)
	public ResponseEntity tag(@RequestBody String json) {
		JsonSlurper slurper = new JsonSlurper()
		def buildData = slurper.parseText(json)
		buildService.provideTag(buildData)
		return ResponseEntity.ok(HttpStatus.OK)
	}

}
