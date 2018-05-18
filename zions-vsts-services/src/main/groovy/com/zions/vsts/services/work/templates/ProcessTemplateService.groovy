package com.zions.vsts.services.work.templates
import com.zions.vsts.services.tfs.rest.GenericRestClient

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service;

@Component
public class ProcessTemplateService  {
	
	
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	
	
    public ProcessTemplateService() {
	}

	public def getWorkitemTemplate(String collection, String project,  String workItemName) {
		def aproject = URLEncoder.encode(project).replace('+', '%20')
		def aworkItemName = URLEncoder.encode(workItemName).replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${aproject}/_apis/wit/workItemTypes/${aworkItemName}",
			headers: [Accept: 'application/json']
			)

		return result;
	}
	public def updateWorkitemTemplate(String collection, String project,  String workItemName, String body) {
		def aproject = URLEncoder.encode(project).replace('+', '%20')
		def aworkItemName = URLEncoder.encode(workItemName).replace('+', '%20')
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${aproject}/_apis/wit/workItemTypes/${aworkItemName}",
			body: body,
			headers: [Accept: 'application/json']
			)
		return result;
	}

}

