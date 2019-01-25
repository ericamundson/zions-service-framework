package com.zions.rm.services.requirements


import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

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
	IGenericRestClient rmGenericRestClient
	
	def queryForModules(String projectURI, String query ) {
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
			
			// Extract module title
			String moduleTitle = module.title
			moduleAttributeMap.put("title", moduleTitle)
			
			// Extract module attributes and members
			module.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					moduleType = parseArtifactAttributes(child, moduleAttributeMap )
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
							}
							else {
								getNonTextArtifact(artifact)
							}
					}
					def j = 1
				}
			}
			
			
			// Add module to result list
			ClmRequirementsModule iModule = new ClmRequirementsModule(moduleType, moduleAttributeMap,orderedArtifacts)
			modules.add(iModule)
		}
		return modules
	}
	
	def nextPage(String url) {
		
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
	
	private void getTextArtifact(ClmModuleElement in_artifact) {
		
		def result = rmGenericRestClient.get(
				uri: in_artifact.about.replace("resources/", "publish/text?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Extract artifact attributes
		result.children().each { artifact ->
			artifact.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					String artifactType = parseArtifactAttributes( child, in_artifact.attributeMap)
					in_artifact.setArtifactType(artifactType)
				}
				else if (iName == "content") {
					// Set primary text
					String primaryText = new groovy.xml.StreamingMarkupBuilder().bind {mkp.yield child.text.richTextBody.children() }
					in_artifact.setDescription(primaryText)
					
					// Check to see if this artifact has an embedded collection
					def collectionIndex = primaryText.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator showContent')
					if (collectionIndex > -1) {
						def hrefIndex = primaryText.substring(collectionIndex).indexOf('href=')
						
					}
					collectionIndex = primaryText.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator minimized')
					if (collectionIndex > -1) {
						def hrefIndex = primaryText.substring(collectionIndex).indexOf('href=')
						String href = primaryText.substring(collectionIndex + hrefIndex + 6)
						def endIndex = href.indexOf("'")
						href = href.substring(1,endIndex)
						in_artifact.collectionArtifacts = getCollectionArtifacts(href)
					}
					
					// Check to see if this artifact has embedded images
				}
			}
		}
		
		return

	}
	private void getNonTextArtifact(ClmModuleElement in_artifact) {
		
		def result = rmGenericRestClient.get(
				uri: in_artifact.about.replace("resources/", "publish/resources?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Extract artifact attributes
		result.children().each { artifact ->
			artifact.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					String artifactType = parseArtifactAttributes( child, in_artifact.attributeMap)
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
		
		return

	}
	private def getCollectionArtifacts(String href) {
		def i = 1
	}
	private String parseArtifactAttributes(NodeChild in_rootCollaborationNode, Map out_attributeMap ) {
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
	
	def getContent(String uri) {
		def result = rmGenericRestClient.get(
			withHeader: true,
			uri: uri,
			contentType: ContentType.BINARY
			);
		return result

	}
}
