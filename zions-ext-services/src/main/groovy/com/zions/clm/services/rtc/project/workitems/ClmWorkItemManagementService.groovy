package com.zions.clm.services.rtc.project.workitems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.clm.services.rest.ClmGenericRestClient;
import com.zions.common.services.rest.IGenericRestClient
import groovy.xml.MarkupBuilder

@Component
public class ClmWorkItemManagementService {

	@Autowired
	IGenericRestClient clmGenericRestClient
	
	public ClmWorkItemManagementService() {
		
	}
	
	
	public getWorkItemsForProject(String project) {
		def query = "workitem/workItem[projectArea/name='${project}']/(id)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		def result = clmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'text/xml'] );
//		File out = new File('tools_wi.xml')
//		def o = out.newDataOutputStream()
//		o << new groovy.xml.StreamingMarkupBuilder().bindNode(result) as String
//		o.close()
		return result
	}
	
	public def nextPage(url) {
		def result = clmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'text/xml'] );
		return result
	}
}
