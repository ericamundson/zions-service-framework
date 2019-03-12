package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Manages VSTS interaction to create/update work items.
 * o submits batch create/update requests
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class WorkManagementService {
	
	final private int BATCHSIZE = 200
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
		
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	
	@Value('${id.tracking.field:}')
	private String idTrackingField
	
	private categoriesMap = [:]

	public WorkManagementService() {
		
	}
	
	def clean(String collection, String project, String query) {
		def wis = getWorkItems(collection, project, query)
		def changelist = []
		wis.workItems.each { wi ->
			deleteWorkitem(wi.url)
		}
	}
	
//	def getWorkItem(String url) {
//		def result = genericRestClient.get(
//			uri: url,
//			contentType: ContentType.JSON,
//			query: [destroy: true, 'api-version': '5.0-preview.3']
//			)
//		return result
//	}

	def getWorkItems(String collection, String project, String aquery) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = [query: aquery]
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/wiql",
			body: body,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0']
			)
		return result

	}
	
	def getWorkItems(String collection, String aquery) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = [query: aquery]
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/wiql",
			body: body,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0']
			)
		return result

	}

	def getWorkItem(String collection, String project, String id) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		//def query = [query: aquery]
		//String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.get(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}",
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0', "\$expand": 'All']
			)
		return result

	}
	
	def getChildren(String collection, String project, String id) {
		def pwi = getWorkItem(collection, project, id)
		def childIds = []
		pwi.relations.each { relation ->
			String rel = "${relation.rel}"
			String url = "${relation.url}"
			if (rel == 'System.LinkTypes.Hierarchy-Forward') {
				int i = url.lastIndexOf('/');
				String cid = null
				if (i != -1) {
					cid = url.substring(i+1);
				}
					
				//def cwi = getWorkitemViaUrl(rel)
				
				childIds.add(cid)
			}
		}
		def children = getListedWorkitems(collection, project, childIds)
		return children
	}
	
	def getCategories(collection, project) {
		if (categoriesMap.size() == 0) {
			def eproject = URLEncoder.encode(project, 'utf-8')
			eproject = eproject.replace('+', '%20')
			//def query = [query: aquery]
			//String body = new JsonBuilder(query).toPrettyString()
			def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitemtypecategories",
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0']
				)
			result.value.each { cat ->
				String catName = "${cat.name}"
				cat.workItemTypes.each { wis -> 
					String typeName = "${wis.name}"
					categoriesMap[typeName] = catName
				}
			}
		}
		return categoriesMap
	}
	
	String getCategory(String collection, String project, def wi) {
		String witype = "${wi.fields.'System.WorkItemType'}"
		def catMap = getCategories(collection, project)
		return catMap[witype]
	}

	def deleteWorkitem(String url) {
		def result = genericRestClient.delete(
			uri: url,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0-preview.3']
			)
		return result

	}
	
	def refreshCache(def collection, def project, def cacheIds) {
		def vstsIds = []
		def idMap = [:]
		int count = 0
		cacheIds.each { id -> 
			def wi = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
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
			cacheManagementService.saveToCache(wi, idMap[count], ICacheManagementService.WI_DATA)
			count++
		}
	}
	
	def getWorkitemViaUrl(String url) {
		def result = genericRestClient.get(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: url,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0']
			)
		return result
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
	def batchWIChanges(def collection, def project, def changeList, def idMap) {
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		int bcount = 0
		def bidMap = [:]
		def bchangeList = []
		int tcount = 0
		int count = changeList.size()
		while (tcount < count) {
			bidMap[bcount] = idMap[tcount]
			bchangeList.add(changeList[tcount])
			bcount++
			if (bcount == BATCHSIZE) {
				doPost(collection, project, bchangeList, bidMap)
				bcount = 0
				bidMap = [:]
				bchangeList = []
			}
			tcount++
		}
		if (bcount > 0) {
			doPost(collection, project, bchangeList, bidMap)
			
		}
	}
	
	private doPost(collection, tfsProject, bchangeList, bidMap) {
		def body = new JsonBuilder(bchangeList).toPrettyString()
		println(body)
		def result = genericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/\$batch",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '4.1']
			
			)
		if (result != null) {
			cacheResult(result, bidMap)
		} else {
			if (checkpointManagementService != null) {
				checkpointManagementService.addLogentry("Batch request failed!")
			}
			log.error("Batch request failed!")
		}

	}
	
	public updateWorkItem(collection, project, id, data) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def body = new JsonBuilder(data).toPrettyString()
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}",
			body: body,
			query: ['api-version': '5.0', bypassRules:true],
			headers: ['Content-Type': 'application/json-patch+json']
			
			)
	}
	
	def cacheResult(result, idMap) {
		int count = 0
		result.value.each { resp ->
			if ("${resp.code}" == '200') {
				def wi = new JsonSlurper().parseText(resp.body)
				cacheManagementService.saveToCache(wi, idMap[count], ICacheManagementService.WI_DATA)
			} else {
				def issue = new JsonSlurper().parseText(resp.body)
				log.error("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
				if (checkpointManagementService != null) {
					checkpointManagementService.addLogentry("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
				}
			}
			count++
		}
	}
	

	
	

}


