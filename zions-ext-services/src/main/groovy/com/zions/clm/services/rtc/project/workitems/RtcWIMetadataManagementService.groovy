package com.zions.clm.services.rtc.project.workitems

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

@Component
@Slf4j
class RtcWIMetadataManagementService {
	@Autowired
	IGenericRestClient clmGenericRestClient

	public RtcWIMetadataManagementService() {
		
	}
	

	def extractWorkitemMetadata(def projectArea, def starterFile) {
		def types = getWorkItemTypes(projectArea)
		types.projectArea.workItemTypes.each { atype ->
			println atype.name.text()
			extractTypeMetadata(projectArea, "${atype.name.text()}", starterFile)
		}
	}
	
	def extractTypeMetadata(def projectArea, def wit, def starterFile) {
		File fInFile = new File(starterFile)
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)
		bXml.'witd:WITD'(application:'Work item type editor',
			version: '1.0',
			'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
			WORKITEMTYPE(name: "${wit}") {
				DESCRIPTION("general work item starter")
				FIELDS {
					fInFile.readLines().each { line ->
						def items = line.split("\\|")
						FIELD(name: "${items[0]}", refname: "RTC.${items[0]}", type: "${items[1]}".trim(), dimension: 'reportable') {
							HELPTEXT "${items[2]}"
						}
						def customAttrs = getCustomAttributes(projectArea, wit)
					}
			
				}
			}
		}

	}
	
	def getCustomAttributes(def projectArea, def wit) {
		def query = "workitem/workItem[projectArea/name = '${projectArea}' and type = '${wit}]/(allExtensions/*)"
		 def encoded = URLEncoder.encode(query, 'UTF-8')
		 encoded = encoded.replace('+', '%20')
		 String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		 def result = clmGenericRestClient.get(
				 uri: uri,
				 headers: [Accept: 'text/xml'] );
 //		def xml = XmlUtil.serialize(result)
 //		println "${xml}"
		 return result
 
	}
	
	def getWorkItemTypes(def projectArea) {
		def query = "workitem/projectArea[name = '${projectArea}']/(contextId|name|workItemTypes/*)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		def result = clmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'text/xml'] );
//		def xml = XmlUtil.serialize(result)
//		println "${xml}"
		return result

	}
	
}
