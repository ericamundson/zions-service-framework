package com.zions.clm.services.work.maintenance.service
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service;

import com.zions.clm.services.rtc.rest.GenericRestClient

@Component
public class FixWorkItemIssuesService  {
	
	
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	
	
    public FixWorkItemIssuesService() {
	}

	public def clearTaskTypeOnDefect(String project) {
		def result = getDefects(project);
		while (true) {
			def workitems = result.workitem
			try {
				workitems.each { workitem ->
					clearTaskTypeField(workitem)
				}
			} catch (err) {
				
			}
			String href = result.@href
			if ("${href}" == '') { break; }
			result = nextPage(href)
		}
		return null;
	}
	
	private def clearTaskTypeField(def workitem) {
		
	}
	
	private def getDefects(String project) {
		def query = "workitem/workItem[projectArea/name='${project}' and type/id='defect']/(id|itemId|summary|state/name|type/name|type/id|teamArea/name|target/id|parent/id|type/name|owner/name)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		String uri = this.genericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		def result = genericRestClient.get(
			uri: uri,
			headers: [Accept: 'text/xml'] );
		println result;
		return result
	}
	
	private nextPage(String url) {
		def result = genericRestClient.get(
			uri: url,
			headers: [Accept: 'text/xml']
			)
		return result
	}

}

