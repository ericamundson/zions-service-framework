package com.zions.vsts.services.work.templates
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.Field
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service;

/**
 * Provides behaviors to manage VSTS/TFS work item meta-data.  
 * o Manage creation of required work item types
 * o Update work item types to ensure required fields
 * o Add new fields to process structure.
 * o Modify work item template layout to display new required fields.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
public class ProcessTemplateService  {
	
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagementService;
	
	@Autowired
	@Value('${type.map.resource:}')
	private String typeMapFileName
	
	@Autowired
	@Value('${name.map.resource:}')
	private String nameMapFileName

	@Autowired
	@Value('${default.wit.name:Task}')
	private String defaultWITName
	
	@Autowired
	@Value('${external.name:Attributes}')
	private String externalName
	
	private def fields = null
	
	private def typeMap = [:]
	private def nameMap = [:]
	
	
    public ProcessTemplateService() {
	}
	
	public def getTypeMapResource(fileName) {
		def data = null
		if (typeMap.size() > 0) return typeMap
		try {
			def s = getClass().getResourceAsStream("/${fileName}")
			JsonSlurper js = new JsonSlurper()
			data = js.parse(s)
			data.typemaps.each { map ->
				typeMap["${map.source}"] = "${map.target}"
			}
		} catch (e) {}
		return typeMap
	}

	public def getNameMapResource(fileName) {
		def data = null
		if (nameMap.size()>0) return nameMap
		try {
			def s = getClass().getResourceAsStream("/${fileName}")
			JsonSlurper js = new JsonSlurper()
			data = js.parse(s)
			data.namemaps.each { map ->
				nameMap["${map.source}"] = "${map.target}"
			}
		} catch (e) {}
		return nameMap
	}

	def getWorkItemTypes(String collection, String project, expand = 'none', projectProperty = 'System.ProcessTemplateType') {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, projectProperty)
		def aproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand':expand]
			)

		return result;

	}
	
	def getWorkItemType(String collection, String project, def refName, expand = 'none', projectProperty = 'System.ProcessTemplateType') {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, projectProperty)
		def aproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${refName}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand':expand]
			)

		return result;

	}

	public def getWorkitemTemplateFields(String collection, String project,  String workItemName) {
		def projectData = projectManagementService.getProject(collection, project)
//		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def aworkItemName = URLEncoder.encode(workItemName, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/workitemtypes/${aworkItemName}/fields",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.3', '$expand': 'all']
			)

		return result;
	}
	
	def getWorkitemTemplateXML(String collection, String project,  String workItemName) {
		def xml = new StringBuilder(), serr = new StringBuilder()
		def proc = "witadmin exportwitd /collection:\"${genericRestClient.getTfsUrl()}/${collection}\" /p:\"${project}\" /n:\"${workItemName}\"".execute()
		proc.waitForProcessOutput(xml, serr)
		return xml
	}
	
	def getField(String url) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: ['$expand': 'all', 'api-version': '4.1'],
			headers: ['Content-Type': 'application/json']
			)
		return result
	}
	
	def queryForField(def collection, def project, def refName, boolean update = true) {
		if (fields == null || update) {
			fields = getFields(collection, project)
		}
		def field = fields.value.find { field ->
			"${field.referenceName}" == "${refName}"
		}
		return field
	}
	
	def getFields(def collection, def project) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/fields",
			headers: ['Content-Type': 'application/json', accept: 'application/json;api-version=5.0-preview.2;excludeUrls=true']
			//query: ['api-version': '5.0-preview.2']
			)
		return result
	}

	def getField(def collection, def project, def refName) {
		def projectData = projectManagementService.getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/fields/${refName}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand': 'all']
			)
		return result
	}

	public def updateWorkitemTemplate(String collection, String project,  String workItemName, String body) {
		def aproject = URLEncoder.encode(project).replace('+', '%20')
		def aworkItemName = URLEncoder.encode(workItemName).replace('+', '%20')
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${aproject}/_apis/wit/workItemTypes/${aworkItemName}",
			body: body,
			headers: [Accept: 'application/json']
			)
		return result;
	}
	
	def updateWorkitemTemplates(def collection, def project, def mapping, def ccmWits) {
		typeMap = getTypeMapResource(typeMapFileName)
		nameMap = getNameMapResource(nameMapFileName)
		def changes = getWITChanges(mapping, ccmWits)
		
		def result = ensureWITChanges(collection, project, changes)
	}
	
	def getWITChanges(def mapping, def ccmWits) {
		
		def changes = []
		def defaultMapping = getDefaultMapping(mapping)
		ccmWits.each { wit ->
//			def mapping = getWITMapping(mapping, wit)
			if (!isExcluded(mapping, wit)) {
				if (!hasMapping(mapping, wit)) {
					def witChanges = [ensureType: "${wit.WORKITEMTYPE.@name}", ensureFields: []]
					witChanges = setFieldChanges(witChanges, wit, defaultMapping)
					changes.add(witChanges)
				} else {
					def witMapping = getWITMapping(mapping, wit)
					def witChanges = [ensureType: "${witMapping.@target}", ensureFields: []]
					witChanges = setFieldChanges(witChanges, wit, witMapping)
					changes.add(witChanges)
				}
			}
		}
		return changes
		
	}
	
	def translateWitChanges(collection, project, String wiName) {
		def wit = getWIT(collection, project, wiName)
		def fields = getWorkitemTemplateFields(collection, project, wiName)
		def layout = getWITLayout(collection, project, wit)
		
		def witChanges = [ensureType: wiName, ensureFields: []]
		
		def fieldMap = [:]
		fields.'value'.each { field ->
			def fieldDetails = getField(collection, project, field.referenceName)
			String type = 'string'
			if (fieldDetails) {
				type = "${fieldDetails.type}".trim()
			}
			def cField = [name: "${field.name}", refName:"${field.referenceName}", type: type, helpText: "${field.description}", page: null, section: null, group: null,suggestedValues:[]]
			field.allowedValues.each { value ->
				cField.suggestedValues.add(value) 
			}
			fieldMap["${field.referenceName}"] = cField
			//witChanges.ensureFields.add(cField)
		}
		
		layout.pages.each { page ->
			page.sections.each { section ->
				section.groups.each { group ->
					group.controls.each { control ->
						def cfield = fieldMap["${control.id}"]
						if (cfield) {
							cfield.page = "${page.label}"
							cfield.section = "${section.id}"
							cfield.group = "${group.label}"
						} else {
							def cField = [name: "${control.label}", refName:"${control.id}", type: '', helpText: 'custom control', page: page.label, section: section.id, group: group.label, control: control]
							if (control.contribution && control.contribution.contributionId) {
								fieldMap["${control.id}"] = cField
								println control.id
							}
						}
					}
				}
			}
		}
		fieldMap.each { key, cfield -> 
			witChanges.ensureFields.add(cfield)
		}
		return witChanges
	}
	
	def getLinkMapping(mapping) {
		def linkMapping = [:]
		mapping.links.link.each { link -> 
			linkMapping["${link.@source}"] = link
		}
		return linkMapping
	}
	
	def getTranslateMapping(def collection, def project, def mapping, def ccmWits) {
		def outType = queryForField(collection, project, 'stuff', true)
		def defaultMapping = getDefaultMapping(mapping)
		def translateMapping = [:]
		ccmWits.each { wit ->
			if (!isExcluded(mapping, wit)) {
				if (!hasMapping(mapping, wit)) {
					def witMap = [source: "${wit.WORKITEMTYPE.@name}", target: "${wit.WORKITEMTYPE.@name}", fieldMaps: [], defaultMap: null]
					witMap = addMappedFields(witMap, defaultMapping)
					witMap = addUnmappedFields(witMap, wit, defaultMapping)
					translateMapping["${wit.WORKITEMTYPE.@name}"] = witMap
				} else {
					def witMapping = getWITMapping(mapping, wit)
					def witMap = [source: "${witMapping.@source}", target: "${witMapping.@target}", fieldMaps: [], defaultMap: null]
					witMap = addMappedFields(witMap, witMapping)
					witMap = addUnmappedFields(witMap, wit, witMapping)
					translateMapping["${witMapping.@source}"] = witMap
				}
			}
		}
		return translateMapping
	}
	
	boolean isExcluded(mapping, wit) {
		if (mapping.exclude == null) return false
		def excluded = mapping.exclude.wit.findAll { awit ->
			"${awit.@name}" == "${wit.WORKITEMTYPE.@name}"
		}
		return excluded.size() != 0
	}
	
	def addMappedFields(witMap, mapping ) {
		mapping.field.each { field ->
			def vstsField = queryForField(null, null, "${field.@target}", false)
			if (vstsField != null) {
				def outType = vstsField.type
				def fieldMap = [source: "${field.@source}", target: "${field.@target}", outName: "${vstsField.name}", outType: outType, valueMap: [], defaultMap: null]
				if (field.value != null) {
					field.value.each { value ->
						fieldMap.valueMap.add([source: "${value.@source}", target: "${value.@target}"])
					}
				}
				field.defaultvalue.each { dV ->
					fieldMap.defaultMap = [target:"${field.defaultvalue.@target}"]					
				}
				witMap.fieldMaps.add(fieldMap)
			
			}
		}
		return witMap
		
	}
	
	def addUnmappedFields(witMap, wit, mapping) {
		wit.WORKITEMTYPE.FIELDS.FIELD.each { field ->
			if (requiresField(field, mapping)) {
				def vstsField = queryForField(null, null, "${externalName}.${field.@refname}", false)
				if (vstsField != null) {
					def outType = vstsField.type
					def outName = "${field.@name}"
					if (nameMap[outName] != null) {
						outName = nameMap[outName]
					}
					def fieldMap = [source: "${field.@refname}", target: "${externalName}.${field.@refname}", outName: outName, outType: outType, valueMap: []]
					witMap.fieldMaps.add(fieldMap)
				}
			}
		}
		return witMap
	}
	
	def setFieldChanges(witChanges, wit, witMapping) {
		wit.WORKITEMTYPE.FIELDS.FIELD.each { field -> 
			if (requiresField(field, witMapping)) {
				def type = 'string'
				String key = "${field.@type}"
				if (typeMap[key] != null) {
					type = typeMap[key]
				}
				def refName = "${externalName}.${field.@refname}"
				def name = "${field.@name}"
				if (nameMap[name] != null) {
					name = nameMap[name]
				}
				def groupName = "${externalName}"
				if ("${type}" == 'html') {
					groupName = "${name}"
				}
				def cField = [name:name, refName:refName, type:type, helpText: field.HELPTEXT.text(), page: "${externalName}", section: 'Section1', group: "${groupName} Fields",suggestedValues:[]]
				if (field.ALLOWEDVALUES != null) {
					field.ALLOWEDVALUES.LISTITEM.each { item ->
						cField.suggestedValues.add("${item.@value}")
					}
				}
				witChanges.ensureFields.add(cField)
			}
		}
		witMapping.newfields.field.each { field ->
			def cField = [name:"${field.@name}", refName:"${field.@refname}", type:"${field.@type}", helpText: "Imported ${field.@name}", page: "${field.@page}", section: "${field.@section}", group: "${field.@group}",suggestedValues:[]]
			witChanges.ensureFields.add(cField)
		}
		return witChanges
	}
	
	
	boolean requiresField(field, witMapping) {
		boolean reqField = true
		if ("${witMapping.@translateUnmappedFields}" == 'false') {
			return false;
		}
		witMapping.field.each { witField ->
			if ("${witField.@source}" == "${field.@refname}") {
				reqField = false
			}
		}
		witMapping.excluded.field.each { mField ->
			if ("${mField.@name}" == "${field.@refname}") {
				reqField = false
			}
		}
		return reqField
	}
	
	def getWITMapping(mapping, wit) {
		def witMapping = null
		mapping.wit.each { witMap ->
			if ("${witMap.@source}" == "${wit.WORKITEMTYPE.@name}") {
				witMapping = witMap
			}
		}
		return witMapping
	}

	
	def getDefaultMapping(mapping) {
		def witMapping = null
		mapping.wit.each { witMap ->
			if ("${witMap.@source}" == 'Default') {
				witMapping = witMap 
			}
		}
		return witMapping
	}
	
	boolean hasMapping(mapping, wit) {
		boolean hasMap = false
		mapping.wit.each { witMap ->
			if ("${witMap.@source}" == "${wit.WORKITEMTYPE.@name}") {
				hasMap = true
			}
		}
		return hasMap

	}
	
	
	def ensureWITChanges(def collection , def project, def changes, boolean updateLayout = false) {
		changes.each { witChange -> 
			def witName = witChange.ensureType
			def wit = ensureWit(collection, project, witName)
			wit = getWIT(collection, project, witName)
			
			witChange.ensureFields.each { witFieldChange ->
				ensureWitField(collection, project, wit, witFieldChange, updateLayout)
			}
		}
		
	}
	
	def ensureWitField(collection, project, wit, witFieldChange, boolean updateLayout = false) {
		String refName = "${witFieldChange.refName}"
		if (witFieldChange.control) {
			def layout = ensureWitFieldLayout(collection, project, wit, null, witFieldChange)
			return
		}
		def field = queryForField(collection, project, witFieldChange.refName)
		if (field == null) {
			def pickList = null
			if (witFieldChange.suggestedValues.size() > 0) {
				pickList = createPickList(collection, project, witFieldChange)
			}
			field = createField(collection, project, witFieldChange, pickList)
		}
		if (field == null) {
			log.error("Unable to create field:  refname: ${witFieldChange.refName}, name: ${witFieldChange.name}")
			return null
		}
		def witField = getWITField(collection, project, wit, field)
		if (witField == null || "${field.referenceName}" != "${witField.referenceName}") {
			witField = addWITField(collection, project, wit.referenceName, field.referenceName)
		}
		if (witField != null && updateLayout) {
			def layout = ensureWitFieldLayout(collection, project, wit, field, witFieldChange)
		}
		
	}
	
	def ensureWitFieldLayout(collection, project, wit, field, witFieldChange) {
		def layout = getWITLayout(collection, project, wit)
		if (witFieldChange.page == null) return
		def changePage = layout.pages.find { page ->
			"${page.label}" == "${witFieldChange.page}"
		}
		if (changePage == null) {
			changePage = createWITPage(collection, project, wit, "${witFieldChange.page}")
		}
		def externalGroup = changePage.sections.find { section ->
				"${section.id}" == "${witFieldChange.section}"
			}.groups.find { group ->
				"${group.label}" == "${witFieldChange.group}"
			}
		if ("${witFieldChange.type}" == 'html' && externalGroup == null) {
			this.addGroupWithControl(collection, project, wit, changePage, field, "${witFieldChange.section}")
		} else if ("${witFieldChange.type}" != 'html') {
			if (externalGroup == null) {
				externalGroup = createWITGroup(collection, project, wit, changePage, witFieldChange.group, witFieldChange.section)
			}
			def control = externalGroup.controls.find { control ->
				"${control.id}" == "${witFieldChange.refName}"
			}
			if (control == null && witFieldChange.control) {
				addExternalControl(collection, project, wit, externalGroup, field, witFieldChange.control)
			} else if (control == null) {
				addExternalControl(collection, project, wit, externalGroup, field)
			}
		}
	}
	
	def addExternalControl(collection, project, wit, externalGroup, field, control = null)
	{
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		//def controlData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		def controlData = null
		if (control) {
			controlData = control
		} else {
			controlData = [order:null, label:field.name, id: field.referenceName, readOnly: false, visible:true, isContribution: false, controlType:null, metadata:null, inherited:null, overridden:null, watermark:null, height:null]
		}
		
		String groupId = URLEncoder.encode(externalGroup.id, 'utf-8').replace('+', '%20')
		def body = new JsonBuilder(controlData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workItemTypes/${wit.referenceName}/layout/groups/${groupId}/Controls/${controlData.id}",
			body: body,
			headers: [accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'
				//referer: "${genericRestClient.getTfsUrl()}/_admin/_process?process-name=${pName}&type-id=${wit.referenceName}&_a=layout"
				]
			//query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}
		
	def addGroupWithControl(collection, project, wit, externalPage, field, section)
	{
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		//def controlData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		def groupData = [id: null, label: field.name, order: null, overridden: null, inherited: null, visible: true, contribution: null, controls: [], isContribution: false]
		def controlData = [order:null, label:field.name, id: field.referenceName, readOnly: false, visible:true, isContribution: false, controlType:null, metadata:null, inherited:null, overridden:null, watermark:null, height:null]
		groupData.controls.add(controlData)
		def body = new JsonBuilder(groupData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout/pages/${externalPage.id}/sections/${section}/groups",
			body: body,
			headers: [accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'
				//referer: "${genericRestClient.getTfsUrl()}/_admin/_process?process-name=${pName}&type-id=${wit.referenceName}&_a=layout"
				]
			//query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}

	def createWITGroup(collection, project, wit, externalPage, name, section = 'Section1') {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		String pageId = URLEncoder.encode(externalPage.id, 'utf-8').replace('+', '%20')
		def groupData = [id: null, label: name, order: null, overridden: null, inherited: null, visible: true, contribution: null, controls: [], isContribution: false]
		def body = new JsonBuilder(groupData).toPrettyString()

		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout/pages/${pageId}/sections/${section}/groups",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1'],
			body: body
			)
		return result

	}

	def createWITPage(collection, project, wit, name) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def pageData = [id: null, label: name, order: null, overridden: null, inherited: null, visible: true, locked: true, pageType: 1, contribution: null, sections: []]
		def section = [id: 'Section1', groups:[], overridden: false]
		pageData.sections.add(section)
		def body = new JsonBuilder(pageData).toPrettyString()

		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout/pages",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1'],
			body: body
			)
		return result

	}
	
	def getWITLayout(collection, project, wit) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result
	}
	
	def createField(collection, project, witFieldChange, pickList) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def pickId = null
		def wiData = [id: "${witFieldChange.refName}", name: "${witFieldChange.name}", type: "${witFieldChange.type}", description: "${witFieldChange.helpText}", pickList: null]
		if (pickList != null) {
			wiData.pickList = pickList
		}
		def body = new JsonBuilder(wiData).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processdefinitions/${processTemplateId}/fields",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result
	}
	
	def createPickList(collection, project, witFieldChange) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def guid = UUID.randomUUID().toString()
		def listData = [id: null, name: "picklist_${guid}", type: 'String', url: null, isSuggested: false, 
			items: []]
		witFieldChange.suggestedValues.each { val ->
			listData.items.add(val)
		}
		def body = new JsonBuilder(listData).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/lists",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}
	
	def ensureWit(collection, project, name) {
		def wit = getWIT(collection, project, name)
		if (wit == null) {
			wit = createWorkitemTemplate(collection, project, name)
		}
		return wit 
	}
	
	def getWIT(collection, project, name) {
		def workItems = getWorkItemTypes(collection, project, 'none')
		def witTemp = null
		workItems.value.each { wit ->
			if ("${name}" == "${wit.name}") {
				witTemp = wit
				return;
			}
		}
		if (witTemp == null) {
			workItems = getWorkItemTypes(collection, project, 'none', 'System.CurrentProcessTemplateId')
			if (workItems != null) {
				workItems.value.each { wit ->
					if ("${name}" == "${wit.name}") {
						witTemp = wit
						return;
					}
				}
			}
			if (witTemp != null) {
				witTemp = updateWIT(collection, project, witTemp)
			}
		} else if ("${witTemp.customization}" == 'system' && witTemp != null)
		{
			witTemp = updateWIT(collection, project, witTemp)			
		}
		if (witTemp != null) {
			def outWit = getWorkItemType(collection, project, witTemp.referenceName, '5')
			if (outWit == null) {
				outWit = getWorkItemType(collection, project, witTemp.referenceName, '5', 'System.CurrentProcessTemplateId')
			}
			return outWit
		}
		return witTemp

	}
	
	def updateWIT(collection, project, witTemp) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def witData = [color: "${witTemp.color}", name: "${witTemp.name}", icon: "${witTemp.icon}", description: "${witTemp.description}.  Update for external system extensions.", inheritsFrom: witTemp.referenceName, isDisabled: false]
		def body = new JsonBuilder(witData).toPrettyString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workItemTypes",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
			)
		return result

	}
	
	def createWorkitemTemplate(collection, project, name) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def defaultWitFields = getWorkitemTemplateFields(collection, project, "${this.defaultWITName}")
		def defaultWit = getWIT(collection, project, "${this.defaultWITName}")
		
		defaultWit.name = name
		defaultWit.inherits = null
		defaultWit.referenceName = null
		defaultWit.id = null
		defaultWit.description = "Translated work item '${name}' from external system."
		defaultWit.url = null
		defaultWit.'class' = null
		defaultWit.isDisabled = false
		defaultWit.color = "${genColor()}"
		defaultWit.icon = 'icon_clipboard'
		
		def body = new JsonBuilder(defaultWit).toPrettyString()
//		File s = new File('defaultwit.json')
//		def w = s.newDataOutputStream()
//		w << body
//		w.close()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
			)
		def actualWit = getWIT(collection, project, name)
		defaultWitFields.value.each { field ->
			addWITField(collection, project, actualWit.referenceName, field.referenceName)
		}
		actualWit = ensureLayout(collection, project, actualWit, defaultWit)
//		def wStr = new JsonBuilder(actualWit).toPrettyString()
//		File s = new File('actualwit.json')
//		def w = s.newDataOutputStream()
//		w << wStr
//		w.close()
		return actualWit;

	}
	
	def ensureLayout(collection, project, wit, defaultWit) {
		def dDataGroups = defaultWit.layout.pages.find { page ->
			"${page.label}" == 'Details'
		}.sections.find { section -> 
			"${section.id}" == 'Section2'
		}.groups
		
		def aDetailsPage = wit.layout.pages.find { page ->
			"${page.label}" == 'Details'
		}
		
		dDataGroups.each { group ->
			def nGroup = addGroup(collection, project, wit, aDetailsPage.id, 'Section2', group.label )
			group.controls.each { control ->
				addControl(collection, project, wit, nGroup.id, control)
			}
		}
	}
	
	def addGroup(collection, project, wit, pageId, sectionId, groupName) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def groupData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		def body = new JsonBuilder(groupData).toPrettyString()
		
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout/pages/${pageId}/sections/${sectionId}/Groups",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result
	}
	
	def addControl(collection, project, wit, groupid, controlData) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		//def controlData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		controlData.inherited = null
		controlData.controlType = null
		def body = new JsonBuilder(controlData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workItemTypes/${wit.referenceName}/layout/groups/${groupid}/Controls/${controlData.id}",
			body: body,
			headers: [accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true' 
				//referer: "${genericRestClient.getTfsUrl()}/_admin/_process?process-name=${pName}&type-id=${wit.referenceName}&_a=layout"
				]
			//query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}
	
	def addWITField(collection, project, witReferenceName, refName) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def witField = [defaultValue: '', referenceName: refName, type: null, readOnly: false, required: false,  allowedGroups: null]
		def body = new JsonBuilder(witField).toPrettyString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${witReferenceName}/fields",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
			)
			
		return result

	}
	
	def getWITField(collection, project, wit, field) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/fields/${field.referenceName}",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
			)
		return result
	}
	
	def genColor() {
		Random random = new Random();
		int nextInt = random.nextInt(256*256*256);
		return String.format("%06x", nextInt);
		
	}

}

