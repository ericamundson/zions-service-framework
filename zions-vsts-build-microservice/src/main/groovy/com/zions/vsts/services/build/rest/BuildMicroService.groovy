package com.zions.vsts.services.build.rest

import com.zions.common.services.model.response.Status
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod

@Component
class BuildMicroService extends AbstractWebSocketMicroService {
	
	@Autowired
	BuildManagementService buildManagementService
	
	@Autowired
	public BuildMicroService(@Value('${websocket.url:}') websocketUrl) {
		super(websocketUrl)
	}

	
	@Override
	public Object processADOData(Object adoData) {
		return null;
	}

	@Override
	public String topic() {
		return 'build.complete';
	}

}
