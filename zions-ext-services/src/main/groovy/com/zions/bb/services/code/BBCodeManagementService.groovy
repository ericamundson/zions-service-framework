package com.zions.bb.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.bb.services.rest.BBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import groovyx.net.http.ContentType

@Component
class BBCodeManagementService {
	@Autowired(required=false)
	private BBGenericRestClient bBGenericRestClient;

	public BBCodeManagementService() {
		
	}
	
	def getKey(String name) {
		ContentType t = null
		def ename = URLEncoder.encode(name, 'UTF-8')
		ename = ename.replace('+', '%20')
		String key = ""
		def resp = bBGenericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${bBGenericRestClient.bbUrl}/rest/api/1.0/projects?name=${ename}",
				headers: [Accept: 'application/json'],
		)
		resp.values.each { p ->
			if ("${p.name}" == "${name}") {
				key = p.key
			}
		}
		return key
	}

	public def getProjectRepoUrls(String project) {
		def urls = []
		def key = getKey(project)
		def reposPart = bBGenericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${bBGenericRestClient.bbUrl}/rest/api/1.0/projects/${key}/repos",
			headers: [Accept: 'application/json'],
			)
		reposPart.values.each { repo ->
			repo.links.'clone'.each { clone ->
				if ("${clone.name}" == 'http') {
					urls.add([url:clone.href,name:repo.name])
				}
			}
		}
		while (true) {
			if (reposPart.isLastPage) {
				break;
			}
			int nextStart = reposPart.nextPageStart
			reposPart = bBGenericRestClient.get (
					contentType: ContentType.JSON,
					uri: "${bBGenericRestClient.bbUrl}/rest/api/1.0/projects/${key}/repos?start=${nextStart}",
					headers: [Accept: 'application/json'],
					)
			reposPart.values.each { repo ->
				repo.links.'clone'.each { clone ->
					if ("${clone.name}" == 'http') {
						urls.add([url:clone.href,name:repo.name])
					}
				}
			}
		}
		return urls
	}
}
