package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Manages VSTS interaction to create/update work items.
 * o submits batch create/update requests
 * @author z091182
 *
 */
@Component
@Slf4j
class WorkManagementService {
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
		
	@Autowired(required=true)
	@Value('${cache.location}')
	private String cacheLocation
	
	@Value('${id.tracking.field}')
	private String idTrackingField

	public WorkManagementService() {
		
	}
	
	def refreshCache(def collection, def project, def cacheIds) {
		def vstsIds = []
		def idMap = [:]
		int count = 0
		cacheIds.each { id -> 
			def wi = getCacheWI(id)
			if (wi != null) {
				String vstsId = "${wi.id}"
				vstsIds.add(vstsId)
				idMap[count] = id
				count++
			}
		}
		def vstsWIs = getListedWorkitems(collection, project, vstsIds)
		count = 0
		vstsWIs.each { wi -> 
			saveState(wi, idMap[count])
			count++
		}
	}
	
	def getListedWorkitems(def collection, def project, def vstsIds) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		//def projectData = projectManagementService.getProject(collection, project)
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${eproject}/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: [ids: vstsIds.join(','), 'api-version': '4.1', '\$expand': 'all' ]
			)
		if (result == null) {
			return []
		}
		return result.value
	}
	
	/**
	 * Submit batch of work item changes to TFS/VSTS.
	 * @param collection
	 * @param project
	 * @param witData - batch of work item changes
	 * @return
	 */
	def batchWIChanges(def collection, def project, def witData, def idMap) {
		def body = new JsonBuilder(witData).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/\$batch",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '4.1']
			
			)
		if (result != null) {
			cacheResult(result, idMap)
		} else {
			log.error("Batch request failed!")
		}
	}
	
	def cacheResult(result, idMap) {
		int count = 0
		result.value.each { resp ->
			if ("${resp.code}" == '200') {
				def wi = new JsonSlurper().parseText(resp.body)
				saveState(wi, idMap[count])
			} else {
				def issue = new JsonSlurper().parseText(resp.body)
				log.error("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
			}
			count++
		}
	}
	
	def saveState(wi, id) {
		File cacheDir = new File(this.cacheLocation)
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		def w  = cacheData.newDataOutputStream()
		w << new JsonBuilder(wi).toPrettyString()
		w.close()
	}
	
	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getCacheWI(id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}

	

}
