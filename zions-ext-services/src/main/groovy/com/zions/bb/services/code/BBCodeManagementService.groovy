package com.zions.bb.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.bb.services.rest.BBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType

/**
 * This class provides behaviors to enable framework interaction with Atlassian Bitbucket 
 * for importing project repos from Bitbucket to VSTS.
 * 
 * @author z091182
 *
 */
@Component
class BBCodeManagementService {
	@Autowired(required=false)
	private IGenericRestClient bBGenericRestClient;

	public BBCodeManagementService() {
		
	}
	
	/**
	 * Get project key for project name.
	 * 
	 * @param name
	 * @return
	 */
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

	/**
	 * Get the GIT url for all of a project's repos.
	 * 
	 * @param project
	 * @return Map = [[url: 'http://git.cs.stuff', name: 'stuff'], [[[url: 'http://git.cs.stuff2', name: 'stuff2']
	 */
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
