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
		
	def itemMap = [93997:'Business Requirement',94000:'Component',94005:'Functional Requirement',94010:'Non Functional Requirement',94001:'Set',94003:'Text',94006:'Use Case']
	
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
	// Get project baselines
	def queryBaselines() {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/baselines?project=$jamaProjectID"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return results.data
	}
	// Get item's children
	def queryItemChildren(def itemID) {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/items/$itemID/children"
		def children = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return children.data
	}
	def queryBaselineItems(def baselineId) {
		String uri = this.jamaGenericRestClient.getJamaUrl() + "/rest/latest/baselines/$baselineId/versioneditems"
		def children = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		return children.data
	}
	// Get Jama Document (we will stick it in a ClmRequirementsModule structure to facilitate Smart Doc creation
	def getComponentDocument(def component) {
		// Create the top-level Jama Document for the Smart Doc
		Map moduleAttributeMap = [:]
		def orderedArtifacts = []
		// Extract module attributes and members
		String moduleType = "Component"
		int depth = 1
		extractAttributes(moduleAttributeMap, component)
		getChildHierarchy(orderedArtifacts, component, depth)
		ClmRequirementsModule clmModule = new ClmRequirementsModule(component.fields.name, null, null, moduleType, null, moduleAttributeMap,orderedArtifacts)
		return clmModule
	}
	// Get Jama Document (we will stick it in a ClmRequirementsModule structure to facilitate Smart Doc creation
	def getBaselineDocument(def baseline) {
		// First create a top-level Jama Document for the Smart Doc
		// Note:  this is still under development.  If it is picked up again, NetReveal Platform 7.7 project seems like a good test bed.  Baseline v0.1
		Map moduleAttributeMap = [:]
		String moduleType = "Component"
		def orderedArtifacts = getBaselineItems(baseline.id)
		ClmRequirementsModule clmModule = new ClmRequirementsModule(baseline.name, null, null, moduleType, null, moduleAttributeMap, orderedArtifacts)
		return clmModule
	}
	def getBaselineItems(def baselineId) {
		def orderedArtifacts = []
		def children = queryBaselineItems(baselineId)
		if (children.size() > 0) {
			// Instantiate element and add to orderedArtifacts
			children.each { child ->
				def seq = child.baselineLocation.sequence
				int depth = 1 + seq.chars().filter({ch -> ch == '.'}).count()
				ClmModuleElement artifact = new ClmModuleElement(child.fields.name, depth, '', 'false', '')
				extractAttributes(artifact.attributeMap, child)
				orderedArtifacts.add(artifact) 
			}
		}
		return orderedArtifacts
	}
	def extractAttributes(Map attributeMap, def item) {
		attributeMap.put('Identifier',item.documentKey)
		attributeMap.put('globalId',item.globalId)
		attributeMap.put('itemType',item.itemType)
		attributeMap.put('Artifact Type',this.itemMap[item.itemType])
		attributeMap.put('createdDate',item.createdDate)
		attributeMap.put('modifiedDate',item.modifiedDate)
		attributeMap.put('createdBy',item.createdBy)
		item.fields.each { field ->
			attributeMap.put(field.key, field.value)
		}

	}
	def getChildHierarchy(def orderedArtifacts, def item, def depth) {
		def children = queryItemChildren(item.id)
		if (children.size() > 0) {
			depth++
			// Instantiate element and add to orderedArtifacts
			children.each { child ->
				ClmModuleElement artifact = new ClmModuleElement(child.fields.name, depth, '', 'false', '')
				extractAttributes(artifact.attributeMap, child)
				orderedArtifacts.add(artifact) 
				if (child.itemType == 94001 ) { // This is a set that may have children
					getChildHierarchy(orderedArtifacts, child, depth)
				}
			}
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
}