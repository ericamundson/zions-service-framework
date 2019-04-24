package com.zions.rm.services.requirements


import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils

/**
 * Handles queries into DNG to navigate object structure of DNG.
 * 
 * Design<p/>
 * <img src="ClmRequirementsManagementService.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * 
 * annotation Componentsdfdsasd
 * annotation Autowired
 * 
 * class ClmRequirementsManagementService {
 * ... Ideas on methods to implement ...
 * + def queryForModules(String project, String query )
 * + def nextPage(String url)
 * }
 * note left: @Component
 * 
 * ClmRequirementsManagementService .. Component: Is as Spring component
 * ClmRequirementsManagementService .. Autowired: Has autowired dependencies
 * ClmRequirementsManagementService o--> RmGenericRestClient: @Autowire rmGenericRestClient
 * 
 * @enduml
 *
 */

@Slf4j
@Component
class ClmRequirementsManagementService {
	@Autowired
	@Value('${clm.url}')
	String clmUrl
	
	@Autowired
	@Value('${clm.pageSize}')
	String clmPageSize
	
	@Autowired
	IGenericRestClient rmGenericRestClient
	
	@Autowired
	IGenericRestClient rmBGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	
	def queryForModules(String projectURI, String query) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/publish/modules?" + query;
		if (query == null || query.length() == 0 || "${query}" == 'none') {  // if no query provided, then at least restrict to the project area
			uri = this.rmGenericRestClient.clmUrl + "/rm/publish/modules?projectURI=" + projectURI;
		}
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
			
		// Define a list of ClmRequirementsModules
		def modules = []
		
		// Extract and instantiate the ClmRequirementsmodules
		result.children().each { module ->
			String moduleType = ""
			def moduleAttributeMap = [:]
			def orderedArtifacts = []
			
			// Extract module title, format, about
			String moduleTitle = module.title
			String moduleFormat = module.format
			String moduleAbout = module.about
			
			// Extract module attributes and members
			module.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					moduleType = parseCollaborationAttributes(child, moduleAttributeMap )
				}
				else if (iName == "moduleContext") {
					def kk = 0
					child.children().each { contextBinding ->
							String about = contextBinding.about
							String artifactTitle = contextBinding.title
							String format = contextBinding.format
							int depth = contextBinding.depth.toInteger()
							String isHeading = contextBinding.isHeading
							String baseUID = contextBinding.core
							
							ClmModuleElement artifact = new ClmModuleElement(artifactTitle, depth, format, isHeading, about)
							artifact.setBaseArtifactURI("${this.rmBGenericRestClient.clmUrl}","${baseUID}")
							
							orderedArtifacts.add(artifact)
							if (format == "Text") {
								// Get artifact details (attributes and links) from DNG
								getTextArtifact(artifact, true)
								// If artifact has embedded collection, add collection members to the module
								if (shouldAddCollectionsToModule(moduleType) && artifact.collectionArtifacts != null && artifact.collectionArtifacts.size > 0) {
									artifact.setDescription(null)  // blank out Description content
									artifact.setArtifactType('Supporting Material')  // Collection container should now be just a Section in the module
									artifact.collectionArtifacts.each { ca ->
										orderedArtifacts.add(ca)
									}
								}
							}
							else {
								getNonTextArtifact(artifact)
							}
					}
					def j = 1
				}
			}
			
			
			// Add module to result list
			ClmRequirementsModule iModule = new ClmRequirementsModule(moduleTitle, moduleFormat, moduleAbout, moduleType, moduleAttributeMap,orderedArtifacts)
			modules.add(iModule)
		}
		return modules
	}

	//This is copied from the ClmWorkItem and Test management services, but we may not need it
	//as we are possibly handling things via BaseQueryHandler's getItems forcing it to saveToCache a new timestamp
	//in scenarios where the previous cache does not exist
	def flushQueries(String projectURI, String oslcNS, String oslcSelect, String oslcWhere) {
		Date ts = new Date()
		cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
		int page = 0
		def currentItems
		new CacheInterceptor() {}.provideCaching(this, "${page}", ts, RequirementQueryData) {
			currentItems = this.queryForArtifacts(projectURI, oslcNS, oslcSelect, oslcWhere)
		}
		while (true) {
			String nextUrl = "${currentItems.ResponseInfo.nextPage.@'rdf:resource'}"
			if (nextUrl == '') break
			page++
			new CacheInterceptor() {}.provideCaching(this, "${page}", ts, RequirementQueryData) {
				currentItems = this.nextPage(nextUrl)
			}
		}
		/*would like to do something more like:
		 String nextUrl = geturl(projectUri blah blah)
		 do {
		 	new CacheInterceptor() {}.provideCaching(this, "${page}", ts, RequirementQueryData) {
			currentItems = this.nextPage(nextUrl)
			}
			nextUrl = "${currentItems.ResponseInfo.nextPage.@'rdf:resource'}"
			page++
		} while (nextUrl != '')
		
	     * but I won't because I'm sure it would break something
	     * just seems like repeated code all over 
		 * 
		 */
	}

	
	//Since the specified values can be autowired, I really feel like maybe
	//there should just be a getUri function and a queryForArtifacts(uri)
	//that returns the below data.  Some of the code seems overly duplicated.
	//not gonna mess with it though.
	def queryForArtifacts(String projectURI, String oslcNS, String oslcSelect, String oslcWhere) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/views?oslc.query=&projectURL=" + this.rmGenericRestClient.clmUrl + "/rm/process/project-areas/" + projectURI + 
					oslcNS + oslcSelect + oslcWhere.replace('zpath',this.rmGenericRestClient.clmUrl) + "&oslc.pageSize=${clmPageSize}";

		uri = uri.replace('<','%3C').replace('>', '%3E')
		log.debug("queryForArtifacts with uri: ${uri}")
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			return null;
		}
	}
	
	
	def queryForFolders(String folderURI) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/folders?oslc.where=public_rm:parent=" + folderURI
		uri = uri.replace('<','%3C').replace('>', '%3E')
		//uri = URLEncoder.encode(uri,'UTF-8')
		//println("Querying folder: " + uri)
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			log.debug ("failed to query folder: "+ folderURI)
			return null;
		}
	}
	
	public def nextPage(url) {
		log.debug("Retrieving page: ${url}")
		def result = rmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			return null;
		}
	}
	
	def queryForWhereUsed() {
		String uri = this.rmBGenericRestClient.clmUrl + "/rs/query/11126/dataservice?report=11099&limit=-1&basicAuthenticationEnabled=true"
		def results = rmBGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/rdf+xml'] );
		if (results != null) {
			def prevID = null
			def whereUsedList = []
			results.children().each { p ->
				def id = "${p.REFERENCE_ID}"
				if (prevID != null && id != prevID) { // Save whereUsed for this id
					cacheManagementService.saveToCache(whereUsedList, prevID, 'whereUsedData')
					whereUsedList.clear()
				}
				whereUsedList.add([name: "${p.MODULE_NAME}", url: "${p.URL2}"])
				
				prevID = id
			}
			return true
		}
		else {
			return false
		}
	}
	
	boolean shouldAddCollectionsToModule(String moduleType) {
		return (moduleType == 'UI Spec' || moduleType == 'Functional Spec')
	}
	
	def getMemberEmail(String url) {
		String emailAddress = ""
		def result = rmGenericRestClient.get(
			uri: url.replace('jts', 'ccm/oslc'),
			headers: [Accept: 'application/xml'] );

		if (result != null) {
			result.children().each { node ->
			// Extract and instantiate the ClmRequirementsmodules
				if (node.name() == 'emailAddress') {
					emailAddress = node
					emailAddress = emailAddress.replace("mailto:","").replace("%40","@")
				}
			}
		}

		return emailAddress
	}
	
	def getTextArtifact(def in_artifact, boolean includeCollections) {
		log.debug("Fetching text artifact")
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/text?resourceURI="),
				headers: [Accept: 'application/xml'] );
		log.debug("Fetching URI: ${in_artifact.getAbout()}")
		// Extract artifact attributes
		result.children().each { artifactNode ->
			parseTopLevelAttributes(artifactNode, in_artifact)
			parseLinksFromArtifactNode(artifactNode, in_artifact)
			artifactNode.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					String artifactType = parseCollaborationAttributes( child, in_artifact.attributeMap)
					in_artifact.setArtifactType(artifactType)
				}
				else if (iName == "content") {
					// Set primary text
					def richTextBody = child.text.richTextBody
					String primaryTextString = new groovy.xml.StreamingMarkupBuilder().bind {mkp.yield richTextBody.children() }
					in_artifact.setDescription(primaryTextString)

					if (includeCollections) {
						// Check to see if this artifact has an embedded collection in "showContent" mode
						def memberHrefs = []
						def collectionIndex = primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator showContent')
						if (collectionIndex > -1) { // Embedded Collection, parse all member hrefs for that collection
							memberHrefs = parseCollectionHrefs(richTextBody)
							in_artifact.setDescription('') // Remove description, since it is now redundant
						}
						
						// Check to see if this artifact has an embedded collection in "minimized" mode
						collectionIndex = primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator minimised')
						if (collectionIndex > -1) { // Minimized Collection, get href for the collection artifact
							String hrefCollection = parseHref(primaryTextString.substring(collectionIndex))
							memberHrefs = getCollectionMemberHrefs(hrefCollection)

						}
						
						// If there are collection members to be retrieved, then retrieve them
						if (memberHrefs != null && memberHrefs.size() > 0) {
							getCollectionArtifacts(in_artifact, memberHrefs)
						}
					}
				}
			}
		}
		return in_artifact

	}

	private String parseHref(String inString) {
		def hrefIndex = inString.indexOf('href=')
		String href = inString.substring(hrefIndex + 6)
		def endIndex = href.indexOf("'")
		return href.substring(0,endIndex)
	}
	private def parseCollectionHrefs(def richTextBody) {
		def memberHrefs = []
		def hrefs = richTextBody.'**'.findAll { p ->
			String src = p.@href
			"${p.name()}" == 'a' && "${src}".startsWith(this.clmUrl)
		}
		hrefs.each { href ->
			memberHrefs << "${href.@href}"
		}
		return memberHrefs
	}
	def getNonTextArtifact(def in_artifact) {
		log.debug("fetching non-text artifact")
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/resources?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Extract artifact attributes
		result.children().each { artifactNode ->
			parseTopLevelAttributes(artifactNode, in_artifact)
			parseLinksFromArtifactNode(artifactNode, in_artifact)
			artifactNode.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					String artifactType = parseCollaborationAttributes( child, in_artifact.attributeMap)
					in_artifact.setArtifactType(artifactType)
				}
				else if (iName == "wrappedResourceURI") {
					// Set primary text
					String primaryText = "<div>Uploaded Attachment</div>"
					in_artifact.setDescription(primaryText)
					String hRef = "${child}"
					in_artifact.setFileHref(hRef)
				}
			}
		}
		
		return in_artifact

	}
	private def getCollectionMemberHrefs(String collectionHref) {
		def result = rmGenericRestClient.get(
				uri: collectionHref.replace("resources/", "publish/collections?resourceURI="),
				headers: [Accept: 'application/xml'] );
		
		def memberHrefs = []
		def links = result.'**'.findAll { p ->
			"${p.name()}" == 'Link' && "${p.@type}" == 'Link' && p.title == 'Unknown Link Type'
		}
		links.each { link ->
			memberHrefs.add("${link.relation}")
		}
		
		return memberHrefs
	}
	private def getCollectionArtifacts(def in_artifact, def memberHrefs) {
		memberHrefs.each { memberHref ->
			def artifact = new ClmModuleElement(null,in_artifact.getDepth(),null,'false',memberHref)

			in_artifact.collectionArtifacts << getTextArtifact(artifact,false)
		}
	}
	
	private void parseLinksFromArtifactNode(def artifactNode, def in_artifact) {
		String modified = artifactNode.collaboration.modified
		String identifier = artifactNode.identifier
		Date modifiedDate = Date.parse("yyyy-MM-dd'T'hh:mm:ss",modified)
		log.debug("Attempting to find and cache links for ${identifier}")
		in_artifact.setLinks(getAllLinks(identifier, modifiedDate, artifactNode))
	}
	
	private void parseTopLevelAttributes(def artifactNode, def in_artifact) {
		String title = "${artifactNode.title}"
		if (title == '') {
			title= "${artifactNode.description}"
		}
		//amending this to deal with null titles in addition to blanks
		if (!title) {
			title= "<blank title>"
		}
		in_artifact.setTitle(title)
		
		if (in_artifact.getBaseArtifactURI() == null || in_artifact.getBaseArtifactURI() == ''){
			String core = "${artifactNode.core}"
			if (core == null || core == '') {
				in_artifact.setBaseArtifactURI(in_artifact.getAbout())
			}
			else {
				// then this is a module element and we need to set the base
				in_artifact.setBaseArtifactURI("${this.rmBGenericRestClient.clmUrl}","${core}")
			}
		}
	}
	
	private String parseCollaborationAttributes(NodeChild in_rootCollaborationNode, Map out_attributeMap ) {
		// Declare type as return argument
		String artifactType
		
		// Extract artifact attributes
		in_rootCollaborationNode.children().each { attr ->
			String attrName = attr.name()
			if (attrName == "creator") {
				String creatorURI = attr.about
				out_attributeMap.put(attrName, creatorURI)
			}
			else if (attrName == "created") {
				String creationDate = attr
				out_attributeMap.put(attrName, creationDate)
			}
			else if (attrName == "attributes") {
				// Get artifact type
				def objTypeAttr = attr.objectType[0].nodeIterator().next()?.attributes()
				artifactType = objTypeAttr['{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name']
				
				// Get custom attributes
				String multiValueAttrName = null
				String multiValueAttrVal = null
				attr.objectType.children().each { custAttr ->
					def attributes = custAttr.nodeIterator().next()?.attributes()
					String custAttrName =  attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name"]
					String custAttrIsEnum = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}isEnumeration"]
					String custAttrIsMultiValued = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}isMultiValued"]
					String custAttrLiteralName = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}literalName"]
					String custAttrValue = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}value"]
					String custAttrVal
					if (custAttrIsEnum == "true") {
						 custAttrVal = custAttrLiteralName
					}
					else {
						 custAttrVal = custAttrValue
					}
					if (custAttrIsMultiValued == 'true') {
						if (multiValueAttrName == null) { // First instance of this attribute
							multiValueAttrName = "${custAttrName}"
							multiValueAttrVal = "${custAttrVal}"
						}
						else if (multiValueAttrName == custAttrName){ // More values for same attribute
							multiValueAttrVal = multiValueAttrVal + ';' + "${custAttrVal}"
						}
						else { // New multivalue attr with previous one to save
							out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
							multiValueAttrName = "${custAttrName}"
							multiValueAttrVal = "${custAttrVal}"
						}
					}
					else { // single value attribute
						if (multiValueAttrName != null) {  // Save pending multivalue attribute
							out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
							multiValueAttrName = null
							multiValueAttrVal = null
						}
						out_attributeMap.put(custAttrName, custAttrVal)
						
					}
				}
				// Add any pending multiValue attribute
				if (multiValueAttrName != null) {  // Save pending multivalue attribute
					out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
				}

			}
		}
		
		return artifactType

	}
	
	// Get Attachment content (binary) from URI
	def getContent(String uri) {
		def result = rmGenericRestClient.get(
			withHeader: true,
			uri: uri,
			contentType: ContentType.BINARY
			);
		return result
	}
	
	//what we need: artifact id and type, then the link set to look through and get all links
	//if how we get the type varies we can pass that in from the parent I suppose
	//if we use datemodified as a parameter as well we can use that to set the cache date
	@Cache(elementType = LinkInfo)
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, def artifactNode) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()
//		String modified = rmItemData.Requirement.modified
//		String identifier = rmItemData.Requirement.identifier
//		if (id == '1570094') {log.print(XmlUtil.serialize(artifactNode))}
		artifactNode.traceability.links.children().each { link ->
			//String itemIdCurrent = child.name()
			String rid = link.identifier //if the target is QM this will be a guid
			String key = link.title //if we just want the string type of link it's .title, uri to type is .linkType
			String module = link.relation.text().split('/')[3]
			log.debug("Found link for Artifact ${id} to ${module} item ${rid}")
			if (module == 'qm') {
				rid = link.alternative
			}
			def info = new LinkInfo(type: key, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'rm', moduleRelated: module)
			links.add(info)
		}
		//autowired cache elementtype=linkinfo not functiong as expected, moving on
		cacheManagementService.saveToCache(links, id, 'LinkInfo')
		return links
	}
	
	
}

//This class is used by the CacheInterceptor to store the direct query results; there are similar identical classes for both CCM and QM.
//I am unsure if the classname is why they are different and that has some impact on how the data is stored in the cache,
//but for consistancy's sake we are making a new data class in the same manner as ClmTestManagementService
@Slf4j
class RequirementQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		log.debug("ReqQueryData serializing result doData")
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		log.debug("ReqQueryData returning serialized result dataValue")
		return new XmlSlurper().parseText(data)
	}

}
