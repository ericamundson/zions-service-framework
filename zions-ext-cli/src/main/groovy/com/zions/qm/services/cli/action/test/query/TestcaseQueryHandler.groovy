package com.zions.qm.services.cli.action.test.query

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TestcaseQueryHandler extends BaseQueryHandler {
	@Value('${testcase.item.filter:allFilter}')
	String tcItemFitler
	
	public def getItems() {
		currentItems = clmTestManagementService.getTestCaseViaQuery(this.qmTcQuery, projectName)
		return currentItems
	}
	public String initialUrl() {
		def query = ''
		def encoded = URLEncoder.encode(this.qmTcQuery, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase";
			
		}
		return uri
	}
	
	public String getFilterName() {
		// TODO Auto-generated method stub
		return this.tcItemFitler;
	}

	
}
