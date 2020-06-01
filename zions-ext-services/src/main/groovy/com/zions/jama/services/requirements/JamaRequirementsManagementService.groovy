package com.zions.jama.services.requirements

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient
import com.zions.rm.services.requirements.ClmRequirementsModule
import com.zions.rm.services.requirements.ClmModuleElement

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import org.apache.commons.io.IOUtils

/**
 * Handles queries into Jama.
 * 
 */

@Slf4j
@Component
class JamaRequirementsManagementService {
	def userEmails
	@Autowired
	@Value('${jama.projectid}')
	String jamaProjectID
		
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired
	IGenericRestClient jamaGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
		
	def itemTypeMap = [93997:'Business Requirement',94000:'Component',94005:'Functional Requirement',94010:'Non Functional Requirement',94001:'Set',94003:'Text',94006:'Use Case',94023:'Attachment',101485:'Attachments',94002:'Folder']
	
	JamaRequirementsManagementService() {
	}
		
	// Get top level components for the project
	def queryComponents() {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/abstractitems?project=$jamaProjectID&itemType=94000"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return results.data
	}
	// Get project data
	def queryProjectData() {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/projects/$jamaProjectID"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return results.data
	}
	// Get project baselines
	def queryBaselines() {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/baselines?project=$jamaProjectID"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return results.data
	}
	// Get item's children
	def queryProjectItems(def project, int startNdx, int max) {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/items?project=${project.id}&startAt=$startNdx&maxResults=$max"
		def result = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return result
	}
	def queryBaselineItems(def baseline, int startNdx, int max) {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/baselines/${baseline.id}/versioneditems?startAt=$startNdx&maxResults=$max"
		def result = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return result
	}
	// Get Jama Document (we will stick it in a ClmRequirementsModule structure to facilitate Smart Doc creation
	def getDocument(def baseline, def project) {
		// First create a top-level Jama Document for the Smart Doc
		Map moduleAttributeMap = [:]
		String docName = getProjectName(project)
		if (baseline) {
			extractRootDocumentAttributes(moduleAttributeMap, 'BL', baseline, docName )
		} else {
			extractRootDocumentAttributes(moduleAttributeMap, 'PRJ', project, docName )
		}
		String moduleType = moduleAttributeMap['Artifact Type']
		def moduleAttachments = []
		def orderedArtifacts = getItems(baseline,project,moduleAttachments)
		ClmRequirementsModule clmModule = new ClmRequirementsModule(docName, null, null, moduleType, null, moduleAttributeMap, orderedArtifacts)
		clmModule.attachments = moduleAttachments
		return clmModule
	}
	String getProjectName(def project) {
		return "${project.fields.name}"
	}
	def getItems(def baseline, def project, def moduleAttachments) {
		def orderedArtifacts = []
		int startNdx = 0
		int maxCount = 50
		int remainCount = 999
		while (remainCount > 0) {
			def result
			if (baseline) {
				result = queryBaselineItems(baseline, startNdx, maxCount)
			}
			else {
				result = queryProjectItems(project, startNdx, maxCount)
			}
			if (result.data.size() > 0) {
				// Instantiate element and add to orderedArtifacts
				result.data.each { child ->
					def location
					if (baseline) {
						location = child.baselineLocation
					} else {
						location = child.location
					}
					if (location) {
						def seq = location.sequence
						int depth = 1 + seq.chars().filter({ch -> ch == '.'}).count()
						ClmModuleElement artifact = new ClmModuleElement(child.fields.name, depth, '', 'false', '')
						extractAttributes(artifact.attributeMap, child)
						if (child.fields.attachment) {
							artifact.attachments.add(child.fields.attachment)
						}
						orderedArtifacts.add(artifact) 
					}
					else if (this.itemTypeMap[child.itemType] == 'Attachment'){
						if (child.fields.attachment) {
							moduleAttachments.add(child.id)
						}
					}
					else {
						println('skipping item, no location in document')
					}
				}
			}
		    int resultCount = result.meta.pageInfo.resultCount
			remainCount = result.meta.pageInfo.totalResults - (startNdx + resultCount)
			startNdx = startNdx + resultCount
		}
		return orderedArtifacts
	}
	def extractRootDocumentAttributes(Map attributeMap, def idPrefix, def item, String projectName) {
		attributeMap.put('api_id',item.id)
		attributeMap.put('Identifier', idPrefix + item.id)
		attributeMap.put('globalId',item.id)
		attributeMap.put('itemType',item.type)
		attributeMap.put('Artifact Type',item.type)
		attributeMap.put('createdDate',item.createdDate)
		attributeMap.put('createdBy',item.createdBy)
		attributeMap.put('name',projectName)
	}
	def extractAttributes(Map attributeMap, def item) {
		attributeMap.put('api_id',item.id)
		attributeMap.put('Identifier',item.globalId)
		attributeMap.put('globalId',item.globalId)
		attributeMap.put('itemType',item.itemType)
		attributeMap.put('Artifact Type',this.itemTypeMap[item.itemType])
		attributeMap.put('createdDate',item.createdDate)
		attributeMap.put('modifiedDate',item.modifiedDate)
		attributeMap.put('createdBy',item.createdBy)
		item.fields.each { field ->
			attributeMap.put(field.key, field.value)
		}
		if (attributeMap['Artifact Type'] == null) {
			log.info("Warning:  Unhandled itemType=$item.itemType")
		}
	}
	def getUserEmail(def id) {
		if (!userEmails) {
			userEmails = getAllUserEmails()
		} 
		return userEmails[id]
	}
	def getAllUserEmails() {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/users?maxResults=50"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		def userEmails = [:]
		if (results) {
			results.data.each { user ->
				userEmails.put(user.id, user.email)
			}
		}
		return userEmails
	}
	def getContent(int itemId) {
//		def itemId = url.find(/.\d+./)
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/attachments/$itemId/file"
		def result = jamaGenericRestClient.get(
			withHeader: true,
			uri: uri,
			headers: ['Accept': ContentType.BINARY] );
		return result
	}
	def getContent(String uri) {
		def result = jamaGenericRestClient.get(
			withHeader: true,
			uri: uri,
			headers: ['Accept': ContentType.BINARY] );
		return result
	}
	def getAttachment(def itemId) {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/attachments/${itemId}"
		def result = jamaGenericRestClient.get(
			uri: uri,
			headers: [Accept: 'application/json'] );
		return result
	}
}