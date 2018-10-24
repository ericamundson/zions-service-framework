package com.zions.qm.services.metadata

import com.zions.qm.services.rest.QmGenericRestClient
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.project.QmProjectManagementService

@Component
@Slf4j
class QmMetadataManagementService {

	String[] metaTypes = ['testplan', 'testsuite', 'testcase', 'testscript']
	def typeMap = ['testplan': 'TEST_PLAN', 'testsuite': 'TEST_SUITE', 'testcase': 'TEST_CASE', 'testscript': 'TEST_SCRIPT']

	@Autowired
	QmGenericRestClient qmGenericRestClient

	@Autowired
	QmProjectManagementService  qmProjectManagementSerivce
	
	public QmMetadataManagementService() {
	}

	def extractQmMetadata(String projectArea, File templateDir) {
		def schema = getQMSchema();
		metaTypes.each { type ->
			def tSchema = findSchema(schema, type)
			String xml = generateMetaData(projectArea,type, schema, tSchema, templateDir)
			File oFile = new File(templateDir, "${type}.xml");
			def w = oFile.newWriter();
			w << "${xml}"
			w.close();
		}
	}

	String generateMetaData(String projectArea, String type, def schema, def tSchema, File templateDir) {
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)
		bXml.'witd:WITD'(application:'Work item type editor',
		version: '1.0',
		'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
			WORKITEMTYPE(name: "${type}") {
				DESCRIPTION("general work item starter")
				FIELDS {
					generateComplexFields(projectArea, type, schema, tSchema.complexType, bXml)
				}
			}
		}
		return writer.toString()
	}

	def generateComplexFields(String projectArea, String type, def schema, def complexType, MarkupBuilder bXml) {
		String extName = "${complexType.complexContent.extension.@base}".toString();
		def parentType = []
		if (extName.length() > 0) {
			parentType = schema.'**'.findAll { node ->
				node.name() == 'complexType'  &&  node.@name == "${extName}"
			}

			if (parentType.size() > 0) {
				generateComplexFields(projectArea, type, schema, parentType[0], bXml)
			}
		}
		complexType.complexContent.extension.sequence.element.each { field ->
			String atype = field.@type
			String aref = field.@ref
			if (atype.length() > 0) {
				bXml.FIELD(name: field.@name, refname: field.@name, type: field.@type) {
				}
			}
			if (aref.length()>0) {
				if ("${field.@ref}" == 'customAttributes') {
					generateCustomFields(projectArea, type, bXml)
				} else {
					bXml.FIELD(name: field.@ref, refname: field.@ref, type: field.@ref) {
					}
				}
			}
			String aname = field.@name
			if ("${aname}" == 'category') {
				generateCategoryFields(projectArea, type, bXml)
			}
		}
	}

	def generateAddedFields(def schema, def tSchema, MarkupBuilder bXml) {
	}
	
	def generateCategoryFields(String projectArea, def type, MarkupBuilder bXml) {
		def cats = this.getCategories(type, projectArea)
		cats.'soapenv:Body'.response.returnValue.values.each { cat ->
			if (!cat.archived) {
				bXml.FIELD(name: cat.name, refname: cat.itemId, type: 'string') {
					ALLOWEDVALUES(expanditems: true) {
						cat.categories.each { value ->
							if (!value.archived) {
								LISTITEM(value: value.name)
							}
						}
					}
				}
			}
		}
		
	}
	
	def generateCustomFields(String projectArea, def type, MarkupBuilder bXml) {
		String mType = this.typeMap[type]
		def attrs = this.getCustomAttributes(mType, projectArea)
		attrs.'soapenv:Body'.response.returnValue.values.each { attr ->
			if (!attr.archived) {
				bXml.FIELD(name: attr.name, refname: attr.identifier, type: 'string') {
				}
			}
		}
	}

	def findSchema(schema, type) {
		def typeSchemas = schema.'**'.findAll { node ->
			//println "${node.parent().@minOccurs}"
			String minOccurs = node.parent().@minOccurs
			node.name() == 'complexType' && "${node.parent().@name}" == "${type}" && node.parent().name() == 'element' && minOccurs.length()==0
		}
		if (typeSchemas.size() > 0) {
			return typeSchemas[0].parent()
		}
		return null;
	}

	def getQMSchema() {
		String uri = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.integration.service.IIntegrationService/schema/qm.xsd"
		def result = qmGenericRestClient.get(
				uri: uri,
				query: [abbreviate: false],
				headers: [Accept: 'text/xml'] );
		return result
	}
	
	def getCustomAttributes(String mType, String projectArea) {
		def projectInfo = qmProjectManagementSerivce.getProject(projectArea)
		String url = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.planning.common.service.rest.ICustomAttributeRestService/customAttributesDTO"
		def attribs = qmGenericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: [scope:mType, resolveValues:false,isNotPurged: true, processAreaUUID: projectInfo.itemId],
			headers: [accept: 'text/json']
		)
		return attribs

	}
	def getCategories(String mType, String projectArea) {
		def projectInfo = qmProjectManagementSerivce.getProject(projectArea)
		String url = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.planning.common.service.rest.ICategoryTypeRestService/categoryTypesDTO"
		def cats = qmGenericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: [itemType:mType, resolveCategories:true,isNotPurged: true, processAreaUUID: projectInfo.itemId, includeGlobal: false],
			headers: [accept: 'text/json']
		)
		return cats

	}
}
