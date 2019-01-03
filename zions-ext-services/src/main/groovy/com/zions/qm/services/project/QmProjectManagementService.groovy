package com.zions.qm.services.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType

@Component
class QmProjectManagementService {
	@Autowired
	IGenericRestClient qmGenericRestClient

	public QmProjectManagementService() {
		
	}
	
	def getProject(String projectArea) {
		String url = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.team.process.internal.service.web.IProcessWebUIService/projectAreasPaged"
		int page = 0;
		int psize = 25;
		int cpsize = 0;
		def thepa = null;
		while (true) {
			def pas = qmGenericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
				query: [hideArchivedProjects:true, owningApplicationKey:'JTS-Sentinel-Id',pageNum: page, pageSize: 25],
				headers: [accept: 'text/json']);
			pas.'soapenv:Body'.response.returnValue.value.elements.each { pa ->
			if ("${pa.summary}" == projectArea) {
					thepa = pa
				}
				cpsize++
			}
			if (cpsize < psize) break;
			if (thepa != null) break;
			cpsize = 0;
			page++
		
		}
		println thepa
		return thepa
	}
}
