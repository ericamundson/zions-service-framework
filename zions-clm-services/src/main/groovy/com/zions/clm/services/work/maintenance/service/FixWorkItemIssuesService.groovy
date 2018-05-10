package com.zions.clm.services.work.maintenance.service
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
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
			def workitems = result.workItem
			workitems.each { workitem ->
				try {
					clearTaskTypeField(workitem)
				} catch (err) {
					println err.message
				}
			}
			String href = result.@href
			if ("${href}" == '') {
				break;
			}
			result = nextPage(href)
		}
		return null;
	}

	private def clearTaskTypeField(def workitem) {
		def childFieldData = getDefectChildField(workitem.id.text());
		XmlSlurper sl = new XmlSlurper()
		def rdf = sl.parseText(childFieldData.data)
		def toDel = []
		rdf.Description.'com.tmb.team.workitem.att.child_tasks1'.each { node ->
			toDel.add(node)
		}
		if (toDel.size() > 0) {
			println "Update defect ${workitem.id.text()}"
			toDel.each { node ->
				node.replaceNode {};
			}
			def body = new groovy.xml.StreamingMarkupBuilder().bindNode(rdf) as String
			String uri = "${this.genericRestClient.clmUrl}/ccm/oslc/workitems/${workitem.id.text()}"
			def query = ['oslc_cm.properties': 'rtc_ext:com.tmb.team.workitem.att.child_tasks1']
			def result = genericRestClient.put(
					uri: uri,
					headers: ['Content-Type': 'application/rdf+xml', Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0','If-Match':childFieldData.etag],
					requestContentType: 'application/xml',
					body: body,
					query: query );
			return result
		}
		return null;
	}

	private def getDefects(String project) {
		def query = "workitem/workItem[projectArea/name='${project}' and type/id='defect']/(id|itemId|stateId|contextId|projectArea/itemId|projectArea/stateId)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		String uri = this.genericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		def result = genericRestClient.get(
				uri: uri,
				headers: [Accept: 'text/xml'] );
		//println result;
		return result
	}

	private getDefectChildField(String id) {
		String uri = "${this.genericRestClient.clmUrl}/ccm/oslc/workitems/${id}"
		def query = ['oslc_cm.properties': 'rtc_ext:com.tmb.team.workitem.att.child_tasks1']
		def result = genericRestClient.getWResponse(
				uri: uri,
				headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'],
				requestContentType: 'application/rdf+xml',
				query: query );
		def childData = new ChildData(data: result.data.text, etag: result.getFirstHeader('ETag').value)
		//println childData.data
		return childData
	}
	private nextPage(String url) {
		def result = genericRestClient.get(
				uri: url,
				headers: [Accept: 'text/xml']
				)
		return result
	}
}

class ChildData {
	def data;
	def etag
}

