package com.zions.rm.services.requirements


import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

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
 * annotation Component
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
							String baseURI = contextBinding.core
							
							ClmModuleElement artifact = new ClmModuleElement(artifactTitle, baseURI, depth, format, isHeading, about)
							orderedArtifacts.add(artifact)
							if (format == "Text") {
								// Get artifact details (attributes and links) from DNG
								getTextArtifact(artifact)
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
	
	def queryForArtifacts(String projectURI, String oslcNS, String oslcSelect, String oslcWhere) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/views?oslc.query=&projectURL=" + this.rmGenericRestClient.clmUrl + "/rm/process/project-areas/" + projectURI + 
					oslcNS + oslcSelect + oslcWhere.replace('zpath',this.rmGenericRestClient.clmUrl) + "&oslc.pageSize=${clmPageSize}";

		uri = uri.replace('<','%3C').replace('>', '%3E')
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			println "artifact xml: " + xml
			return new XmlSlurper().parseText(xml)
		}
		else {
			return null;
		}
	}
	
	def queryForFolders(String folderURI) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/folders?oslc.where=public_rm:parent=" + folderURI
		//uri = uri.replace('<','%3C').replace('>', '%3E')
		println("Querying folder: " + uri)
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			return null;
		}
	}
	
	public def nextPage(url) {
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
		def result = rmBGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/rdf+xml'] );
		return result;
	}
	
	boolean shouldAddCollectionsToModule(String moduleType) {
		return (moduleType == 'UI Spec')
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
	
	def getTextArtifact(def in_artifact) {
		
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/text?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Extract artifact attributes
		result.children().each { artifactNode ->
			parseTopLevelAttributes(artifactNode, in_artifact)
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

					// Check to see if this artifact has an embedded collection in "showContent" mode
					def memberHrefs = []
					def collectionIndex = primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator showContent')
					if (collectionIndex > -1) { // Embedded Collection, parse all member hrefs for that collection
						memberHrefs = parseCollectionHrefs(richTextBody)
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
		
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/resources?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Extract artifact attributes
		result.children().each { artifactNode ->
			parseTopLevelAttributes(artifactNode, in_artifact)
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
	private void getCollectionMemberHrefs(String collectionHref) {
		def result = rmGenericRestClient.get(
				uri: collectionHref.replace("resources/", "publish/collections?resourceURI="),
				headers: [Accept: 'application/xml'] );
		
		def memberHrefs = []
		def links = result.'**'.findAll { p ->
			"${p.name()}" == 'relation' //&& "${p.parent.name}" == 'Link'
		}
		links.each { link ->
			memberHrefs.add("${link}")
		}
		
		return
	}
	private def getCollectionArtifacts(def in_artifact, def memberHrefs) {
		memberHrefs.each { memberHref -> 
			def artifact = new ClmModuleElement(null,null,in_artifact.getDepth(),null,'false',memberHref)

			in_artifact.collectionArtifacts << getTextArtifact(artifact)
		}
	}
	
	private void parseTopLevelAttributes(def artifactNode, def in_artifact) {
		String title = "${artifactNode.title}"
		if (title == '') {
			title= "${artifactNode.description}"
		}
		if (title == '') {
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
				in_artifact.setBaseArtifactURI("${this.rmBGenericRestClient.clmUrl}/rm/resources/${core}")
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
				attr.objectType.children().each { custAttr ->
					def attributes = custAttr.nodeIterator().next()?.attributes()
					String custAttrName =  attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name"]
					String custAttrIsEnum = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}isEnumeration"]
					String custAttrLiteralName = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}literalName"]
					String custAttrValue = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}value"]
					String custAttrVal
					if (custAttrIsEnum == "true") {
						 custAttrVal = custAttrLiteralName
					}
					else {
						 custAttrVal = custAttrValue
					}
					out_attributeMap.put(custAttrName, custAttrVal)
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
}
