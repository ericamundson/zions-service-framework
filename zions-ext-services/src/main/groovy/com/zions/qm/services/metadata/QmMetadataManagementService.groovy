package com.zions.qm.services.metadata

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.project.QmProjectManagementService

/**
 * Handles generating xml representation of QM project meta-data.  
 * o Incorporates project area configuration to define output fields of various test types.
 * 
 * @author z091182
 *
 */
@Slf4j
@Component
class QmMetadataManagementService {

	def xsdTypes = ['TestPlan':'testplan', 'TestSuite':'testsuite', 'TestCase':'testcase', 'TestScript':'testscript', 'TestCaseExecutionRecord':'executionworkitem', 'TestSuiteExecutionRecord':'suiteexecutionrecord', 'TestCaseExecutionResult':'executionresult', 'TestSuiteExecutionResult':'testsuitelog','Step':'step']
	def customAttributeMapType = ['TestPlan': 'TEST_PLAN', 'TestSuite': 'TEST_SUITE', 'TestCase': 'TEST_CASE', 'TestScript': 'TEST_SCRIPT', 'TestCaseExecutionRecord':'TESTCASE_EXECUTIONRECORD', 'TestSuiteExecutionRecord':'TESTSUITE_EXECUTIONRECORD', 'TestCaseExecutionResult':'TESTCASE_EXECUTIONRESULT','TestSuiteExecutionResult':'TESTSUITE_EXECUTIONRESULT','Step':'MANUAL_STEP']
	//def categoriesMapType = ['testplan': 'TestPlan', 'testsuite': 'TestSuite', 'testcase': 'TestCase', 'testscript': 'TestScript']
	def schemaMap = [:]
	
	@Autowired(required=true)
	IGenericRestClient qmGenericRestClient

	@Autowired
	QmProjectManagementService  qmProjectManagementSerivce

	public QmMetadataManagementService() {
	}

	/**
	 * Entry point method to generate meta-data xml.
	 * 
	 * @param projectArea
	 * @param templateDir
	 * @return
	 */
	def extractQmMetadata(String projectArea, File templateDir) {
		def schema = getQMSchema();
		xsdTypes.each { key, type ->
			def tSchema = findSchema(schema, type)
			String xml = generateMetaData(projectArea,key, schema, tSchema, templateDir)
			File oFile = new File(templateDir, "${key}.xml");
			def w = oFile.newWriter();
			w << "${xml}"
			w.close();
		}
	}

	/**
	 * @param projectArea - Project area name
	 * @param key - key index to xml element name.
	 * @param schema
	 * @param tSchema
	 * @param templateDir
	 * @return
	 */
	String generateMetaData(String projectArea, String key, def schema, def tSchema, File templateDir) {
		def type = this.xsdTypes[key]
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)
		bXml.'witd:WITD'(application:'Work item type editor',
		version: '1.0',
		'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
			WORKITEMTYPE(name: "${key}") {
				DESCRIPTION("general work item starter")
				FIELDS {
					generateComplexFields(projectArea, key, schema, tSchema.complexType, bXml)
				}
			}
		}
		return writer.toString()
	}

	String stripNS(String name) {
		String[] parts = name.split(':')
		if (parts.size()>1) {
			return parts[1]
		}
		return parts[0]
	}


	/**
	 * @param projectArea
	 * @param key
	 * @param schema
	 * @param complexType
	 * @param bXml
	 * @return
	 */
	def generateComplexFields(String projectArea, String key, def schema, def complexType, MarkupBuilder bXml) {
		String type = this.xsdTypes[key]
		String extName = "${complexType.complexContent.extension.@base}".toString();
		def parentType = []
		if (extName.length() > 0) {
			parentType = schema.'**'.findAll { node ->
				node.name() == 'complexType'  &&  node.@name == "${extName}"
			}

			if (parentType.size() > 0) {
				generateComplexFields(projectArea, key, schema, parentType[0], bXml)
			}
		}

		complexType.complexContent.extension.sequence.element.each { field ->
			String atype = field.@type
			String aref = field.@ref
			if (atype.length() > 0 && inTemplate(field, key, projectArea)) {
				atype = stripNS(atype)
				String name = stripNS("${field.@name}")
				bXml.FIELD(name: name, refname: name, type: atype) {
				}
			}
			if (aref.length()>0) {
				if ("${field.@ref}" == 'customAttributes') {
					generateCustomFields(projectArea, key, bXml)
				} else {
					String fType = "${field.@ref}"
					String reftype = determineType(schema, fType)
					String cardinality = determineCardinality(schema,fType)
					if (reftype == null) {
						reftype = field.ref
					}
					reftype = stripNS(reftype)
					String name = stripNS("${field.@ref}")
					bXml.FIELD(name: name, refname: name, type: reftype) {
					}
				}
			}
			String aname = field.@name
			if ("${aname}" == 'category') {
				generateCategoryFields(projectArea, type, bXml)
			} else if (aname.length()>0 && atype.length()==0) {
				
			}
		}
		complexType.sequence.element.each { field ->
			String atype = field.@type
			String aref = field.@ref
			if (atype.length() > 0 && inTemplate(field, key, projectArea)) {
				atype = stripNS(atype)
				String name = stripNS("${field.@name}")
				bXml.FIELD(name: name, refname: name, type: atype) {
				}
			}
			if (aref.length()>0) {
				if ("${field.@ref}" == 'customAttributes') {
					generateCustomFields(projectArea, key, bXml)
				} else {
					String fType = "${field.@ref}"
					String reftype = determineType(schema, fType)
					String cardinality = determineCardinality(schema,fType)
					if (reftype == null) {
						reftype = field.ref
					}
					reftype = stripNS(reftype)
					String name = stripNS("${field.@ref}")
					bXml.FIELD(name: name, refname: name, type: reftype) {
					}
				}
			}
			String aname = field.@name
			if ("${aname}" == 'category') {
				generateCategoryFields(projectArea, type, bXml)
			} else {
				
			}
			
		}
	}

	boolean inTemplate(def field, String key, String projectArea) {
		return true
	}

	/**
	 * @param projectArea
	 * @param key
	 * @param bXml
	 * @return
	 */
	def generateCategoryFields(String projectArea, def key, MarkupBuilder bXml) {
		def cats = this.getCategories(key, projectArea)
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

	/**
	 * @param projectArea
	 * @param key
	 * @param bXml
	 * @return
	 */
	def generateCustomFields(String projectArea, def key, MarkupBuilder bXml) {
		String mType = this.customAttributeMapType[key]
		def attrs = this.getCustomAttributes(mType, projectArea)
		attrs.'soapenv:Body'.response.returnValue.values.each { attr ->
			if (!attr.archived) {
				bXml.FIELD(name: attr.name, refname: attr.identifier, type: 'string') {
				}
			}
		}
	}

	/**
	 * @param schema
	 * @param type
	 * @return
	 */
	def findSchema(schema, type) {
		def typeSchemas = schema.'**'.findAll { node ->
			//println "${node.parent().@minOccurs}"
			String minOccurs = node.parent().@minOccurs
			def cTypeChild = node.children().findAll { child ->
				child.name() == 'complexContent'
			}
			cTypeChild.size() > 0 && node.name() == 'complexType' && "${node.parent().@name}" == "${type}" && node.parent().name() == 'element' && minOccurs.length()==0
		}
		if (typeSchemas.size() > 0) {
			return typeSchemas[0].parent()
		}
		return null;
	}

	/**
	 * @return
	 */
	def getQMSchema() {
		String uri = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.integration.service.IIntegrationService/schema/qm.xsd"
		def result = qmGenericRestClient.get(
				uri: uri,
				query: [abbreviate: false],
				headers: [Accept: 'text/xml'] );
		schemaMap['qm'] = result
		return result
	}

	/**
	 * @param mType
	 * @param projectArea
	 * @return
	 */
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

	/**
	 * @param mType
	 * @param projectArea
	 * @return
	 */
	def getCategories(String type, String projectArea) {
		//String cType = this.categoriesMapType[type]
		def projectInfo = qmProjectManagementSerivce.getProject(projectArea)
		String url = "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.planning.common.service.rest.ICategoryTypeRestService/categoryTypesDTO"
		def cats = qmGenericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
				query: [itemType:type, resolveCategories:true,isNotPurged: true, processAreaUUID: projectInfo.itemId, includeGlobal: false],
				headers: [accept: 'text/json']
				)
		return cats

	}

	String determineType(def schema, String typeName) {
		String[] typeParts = typeName.split(':')
		String name = ""
		String schemaElement = ""
		if (typeParts.length == 1) {
			schemaElement = 'qm'
			name = typeParts[0]
		} else {
			schemaElement = typeParts[0]
			name = typeParts[1]
		}

		if (schemaElement == 'xs') {
			return name
		}
		def mSchema = loadSchema(schema, schemaElement)
		if (mSchema != null) {
			def element = mSchema.depthFirst().find { node ->
				node.@name == name
			}
			if (element != null) {
				String atype = "${element.@type}"
				if (atype.length()>0) {
					return atype
				}
				def cType = element.depthFirst().find { child ->
					child.name() == 'complexType'
				}
				if (cType != null) {
					def attr = cType.depthFirst().find { ct ->
						ct.name() == 'attribute'
					}
					if (attr != null) {
						String attrType = "${attr.@type}"
						if (attrType.length()>0) {
							return attrType
						}
						String attrRef = "${attr.@ref}"

						return determineType(mSchema, attrRef)
					}
					return 'string'
				}
			}
		} else {
			return 'string'
		}
		return null
	}

	String determineCardinality(def schema, String fType) {

	}

	def loadSchema(schema, String schemaElement) {
		if (schemaMap[schemaElement] != null) {
			return schemaMap[schemaElement]
		}
		def mschema = qmGenericRestClient.get(
				uri: "${qmGenericRestClient.qmUrl}/qm/service/com.ibm.rqm.integration.service.IIntegrationService/schema/${schemaElement}.xsd",
				query: [abbreviate: false],
				headers: [Accept: 'text/xml'] );

		schemaMap[schemaElement] = mschema
		return mschema
	}
}
