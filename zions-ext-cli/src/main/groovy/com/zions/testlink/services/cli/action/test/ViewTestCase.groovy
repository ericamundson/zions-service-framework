package com.zions.testlink.services.cli.action.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.testlink.services.test.TestLinkClient
import groovy.json.JsonBuilder

@Component
class ViewTestCase implements CliAction {
	
	@Autowired(required=false)
	TestLinkClient testLinkClient
	
	@Value('${workitem.id:0}')
	int workItemId

	//@Override
	public Object execute(ApplicationArguments args) {
		def tc = testLinkClient.getTestCase(workItemId-1, null, null)
		if (tc) {
			String out = new JsonBuilder(tc).toPrettyString()
			println out
		}
		return null;
	}

	//@Override
	public Object validate(ApplicationArguments args) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
