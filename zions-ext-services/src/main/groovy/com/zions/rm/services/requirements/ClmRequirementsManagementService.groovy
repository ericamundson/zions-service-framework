package com.zions.rm.services.requirements

import com.zions.rm.services.rest.RmGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import groovy.xml.XmlUtil

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
	RmGenericRestClient rmGenericRestClient
	
	def queryForModules(String projectURI, String query ) {

		String uri = this.rmGenericRestClient.rmUrl + "/rm/publish/modules?" + query;
		if (query == null || query.length() == 0 || "${query}" == 'none') {  // if no query provided, then at least restrict to the project area
			uri = this.rmGenericRestClient.rmUrl + "/rm/publish/modules?projectURI=" + projectURI;
		}
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
			
		// Define a list of ClmRequirementsModules
		def modules = []
		
		result.children().each { module ->

			// Extract module title
			String title = module.title
			
			// Extract module attributes and members
			def attributeMap = [:]
			def orderedMembers = []
			module.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					child.children().each { attr ->
						String attrName = attr.name()
						if (attrName == "creator") {
							String creatorName = attr.title
							attributeMap.put(attrName, creatorName)
						}
						else if (attrName == "created") {
							String creationDate = attr
							attributeMap.put(attrName, creationDate)
						}
						else if (attrName == "attributes") {
							// Get artifact type
							def objTypeAttr = attr.objectType[0].nodeIterator().next()?.attributes()
							String artifactType = objTypeAttr['{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name']
							attributeMap.put("Artifact Type", artifactType)
							
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
								attributeMap.put(custAttrName, custAttrVal)
							}
						}
					}
				}
				else if (iName == "moduleContext") {
					child.children().each { contextBinding ->
						String about = contextBinding.about
						int identifier = contextBinding.identifier.toInteger()
						String artifactTitle = contextBinding.title
						String format = contextBinding.format
						int depth = contextBinding.depth.toInteger()
						String isHeading = contextBinding.isHeading
						String baseURI = contextBinding.core
						
						ClmModuleMember member = new ClmModuleMember(identifier, artifactTitle, baseURI, depth, format, isHeading, about )
						orderedMembers.add(member)
					}
					def j = 1
				}
			}
			
			
			// Add module to result list
			ClmRequirementsModule iModule = new ClmRequirementsModule(title,attributeMap,orderedMembers)
			modules.add(iModule)
		}
		return modules
	}
	
	def nextPage(String url) {
		
	}

}
