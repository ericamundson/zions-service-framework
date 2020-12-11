package com.zions.vsts.services.work.templates
import com.zions.common.services.logging.FlowInterceptor
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
public class ProcessTemplateService {
	
	
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
	// Extracts all WIT controls
	def translateWitChanges(collection, project, String wiName) {
		def wit = getWIT(collection, project, wiName)
		def fields = getWorkitemTemplateFields(collection, project, wiName)
//		RH - removed separate call to get layout, since we get it with WIT expand=layout		
//		def layout = getWITLayout(collection, project, wit)
		
		def witChanges = [ensureType: wiName, ensureFields: []]
		
		def fieldMap = [:]
		fields.'value'.each { field ->
			def fieldDetails = getField(collection, project, field.referenceName)
			String type = 'string'
			if (fieldDetails) {
				type = getFieldType(fieldDetails)
			}
			def cField = [name: "${field.name}", label: "${fieldDetails.label}", refName:"${field.referenceName}", type: type, helpText: "${fieldDetails.description}", page: null, section: null, group: null,suggestedValues:[]]
			field.allowedValues.each { value ->
				cField.suggestedValues.add(value) 
			}
			fieldMap["${field.referenceName}"] = cField
			//witChanges.ensureFields.add(cField)
		}
		
		wit.layout.pages.each { page ->
			page.sections.each { section ->
				section.groups.each { group ->
					group.controls.each { control ->
						def cfield = fieldMap["${control.id}"]
						if (cfield) {
							cfield.page = "${page.label}"
							cfield.section = "${section.id}"
							cfield.group = "${group.label}"
							cfield.label = "${control.label}"
						} else {
							cfield = [name: "${control.label}", label: null, refName:"${control.id}", type: '', helpText: 'custom control', page: page.label, section: section.id, group: group.label, control: control]
							if (control.contribution && control.contribution.contributionId) {
								fieldMap["${control.id}"] = cfield
								println control.id
							}
						}
						// Output controls/fields that are in groups to preserve order
						if (cfield && 
							cfield.page != "History" && 
							cfield.page != "Links" && 
							cfield.page != "Attachments" &&
							cfield.name != "") 
							witChanges.ensureFields.add(cfield)
					}
				}
			}
		}
		// Output State (only System field not on the layout that we care about)
		witChanges.ensureFields.add(fieldMap['System.State'])
		
		return witChanges
	}
	def getFieldType(field) {
		def type = "${field.type}".trim()
		if (type == 'string' && field.isIdentity == true) {
			type = 'identity'
		}
		return type
	}
	def getLinkMapping(mapping) {
		def ilinkMapping = [:]
		mapping.links.link.each { link -> 
			ilinkMapping["${link.@source}"] = link
		}
		return ilinkMapping
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
	
	// Create/update WIT using WIT definition (changes)
	def ensureWITChanges(def collection , def project, def changes, boolean updateLayout = false, boolean clearWIT = false) {
		changes.each { witChange -> 
			def witName = witChange.ensureType
			log.info("Importing $witName...")
			def wit = ensureWit(collection, project, witName, clearWIT)
//			wit = getWIT(collection, project, witName)
			
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
		if (field == null) {  // New field
			def pickList = null
			if (witFieldChange.suggestedValues.size() > 0) {
				pickList = createPickList(collection, project, witFieldChange)
			}
			field = createField(collection, project, witFieldChange, pickList)
			// Bug??? API does not return the field referenceName in the response
			// So have to fetch field to get full field content
			field = queryForField(collection, project, witFieldChange.refName)
		} 
		else { // Field already exists, need to update it
			// Make sure field type matches
			if (!checkTypeMatch(witFieldChange,field)) {
				log.error("Field type does not match existing field:  refname: ${witFieldChange.refName}, name: ${witFieldChange.name}")
				return null
			}
			
			// if there is a picklist, make sure it is up to date
			def pickList = null
			if (witFieldChange.suggestedValues.size() > 0 && field.picklistId) {
				pickList = updatePickList(collection, project, witFieldChange, field.picklistId)
			}
			if ("${field.referenceName}".substring(0,6) == "Custom.")
				field = updateField(collection, project, witFieldChange, pickList, field)

		}
		
		// Make sure the field has been added to this WIT, and update the field layout on the WIT editor form
		if (field == null) {
			log.error("Unable to create field:  refname: ${witFieldChange.refName}, name: ${witFieldChange.name}")
			return null
		}
		def witField = getWITField(collection, project, wit, field)
		if (witField == null || "${field.referenceName}" != "${witField.referenceName}") {
			witField = addWITField(collection, project, wit.referenceName, field.referenceName, field.type)
		}
		if (witField != null && updateLayout) {
			def layout = ensureWitFieldLayout(collection, project, wit, field, witFieldChange)
		}
		
	}
	
	// Make sure import field type matches existing field type in target org
	def checkTypeMatch(def witFieldChange, def field) {
		if (witFieldChange.type == field.type)
			return true
		else if (witFieldChange.type == 'identity' && field.isIdentity == true)
			return true
		else
			return false
	}
	
	def ensureWitFieldLayout(collection, project, wit, field, witFieldChange) {
		//	Check if new page needs to be created
		if (witFieldChange.page == null) return
		def changePage = wit.layout.pages.find { page ->
			"${page.label}" == "${witFieldChange.page}"
		}
		if (changePage == null) {
			changePage = createWITPage(collection, project, wit, "${witFieldChange.page}")
			// Refresh WIT layout to include new page
			wit.layout.pages.add(changePage)
		}
		
		//	Check if new group needs to be created or moved
		def foundGroup = findGroup(changePage, witFieldChange)
		def group
		if (foundGroup == null) {  // group does not exist
			if ("${witFieldChange.type}" == 'html')
				group = addGroupWithControl(collection, project, wit, changePage, field, "${witFieldChange.section}")
			else
				group = createWITGroup(collection, project, wit, changePage, witFieldChange.group, witFieldChange.section)
			// Refresh WIT layout to include new group
			def changeSection = changePage.sections.find { section ->
								"${section.id}" == "${witFieldChange.section}" }
			changeSection.groups.add(group)
		}
		else if ("${foundGroup.section.id}" != "${witFieldChange.section}") { // Group exists on different page
			group = moveWITGroup(collection, project, wit, changePage, foundGroup.group, witFieldChange.section, foundGroup.section)
			// Refresh WIT layout to reflect move to new section
			def changeSection = changePage.sections.find { section ->
								"${section.id}" == "${witFieldChange.section}" }
			changeSection.groups.add(group)
			int i = 0
			int iRemove
			foundGroup.section.groups.each { grp ->
				if ("${grp.id}" == "${group.id}") {
					iRemove = i
				}
				i++
			}
			foundGroup.section.groups.removeAt(iRemove)
		}
		else { // group exists on target page
			group = foundGroup.group
		}
		def control = group.controls.find { control ->
			"${control.id}" == "${witFieldChange.refName}"
		}
		// Add/update the control as needed
		if (control == null) {
			ensureExternalControl(collection, project, wit, group, witFieldChange, genericRestClient.&post)
		}
		else {
			boolean updateRequired = ("${control.label}" != "${witFieldChange.label}")  // more conditions to be added
			if (updateRequired) {
				ensureExternalControl(collection, project, wit, group, witFieldChange, genericRestClient.&patch)
			}
		}
	}
	// Find the group in the page layout and return the Section that contains it
	def findGroup(changePage, witFieldChange) {
		def foundGroup, foundSection
		changePage.sections.each { section ->
			def result
			if ("${witFieldChange.type}" == 'html') {
				result = section.groups.find { group ->
						group.controls.size() > 0 && "${group.controls[0].id}" == "${witFieldChange.refName}"}
			}
			else {
				result = section.groups.find { group ->
						"${group.label}" == "${witFieldChange.group}"}
			}
			if (result) {
				foundGroup = result
				foundSection = section
			}
		}

		if (foundGroup) 
			return [group: foundGroup, section: foundSection]
		else
			return null
	}
	
	def ensureExternalControl(collection, project, wit, externalGroup, field, Closure operation)
	{
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		//def controlData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		def controlData = null
		if (field.control) {
			controlData = field.control
		} else {
			controlData = [order:null, label:field.label, id: field.refName, readOnly: false, visible:true, isContribution: false, controlType:null, metadata:null, inherited:null, overridden:null, watermark:null, height:null]
		}
		
		String groupId = URLEncoder.encode(externalGroup.id, 'utf-8').replace('+', '%20')
		def body = new JsonBuilder(controlData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = operation(
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
		def controlData = [order:null, label:field.label, id: field.referenceName, readOnly: false, visible:true, isContribution: false, controlType:null, metadata:null, inherited:null, overridden:null, watermark:null, height:null]
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
	
	def moveWITGroup(collection, project, wit, externalPage, group, section, oldSection) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		String pageId = URLEncoder.encode(externalPage.id, 'utf-8').replace('+', '%20')
		def groupData = [id: group.id, label: group.label, order: 1, visible: true, contribution: null, controls: null, isContribution: false]
		def body = new JsonBuilder(groupData).toPrettyString()

		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}/layout/pages/${pageId}/sections/${section}/groups/${group.id}",
			headers: [accept: 'application/json'],
			query: ['removeFromSectionId': "${oldSection.id}",'api-version': '6.1-preview.1'],
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
	
	def deleteWit(collection, project, wit) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${wit.referenceName}",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
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
		if (result) 
			
		return result
	}
	def updateField(collection, project, witFieldChange, pickList, field) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def pickId = null
		def wiData = [id: "${field.referenceName}", name: "${field.name}", type: "${field.type}", description: "${witFieldChange.helpText}", pickList: null]
		if (pickList != null) {
			wiData.pickList = pickList
		}
		def body = new JsonBuilder(wiData).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processdefinitions/${processTemplateId}/fields/${witFieldChange.refName}",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result
	}

	def createPickList(collection, project, witFieldChange) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def guid = UUID.randomUUID().toString()
		def listData = [id: null, name: "picklist_${guid}", type: 'String', url: null, isSuggested: true, 
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
	def updatePickList(collection, project, witFieldChange, pickListId) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def guid = UUID.randomUUID().toString()
		def pickList = [id: pickListId, items: [], isSuggested: true]
		witFieldChange.suggestedValues.each { val ->
			pickList.items.add(val)
		}
		def body = new JsonBuilder(pickList).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/lists/${pickListId}",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}

	def ensureWit(collection, project, name, boolean clearWIT) {
		def wit = getWIT(collection, project, name)
		if (clearWIT && wit) {
			deleteWit(collection, project, wit)
		}
		if (wit == null || clearWIT) {
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
			def outWit = getWorkItemType(collection, project, witTemp.referenceName, 'layout')
			if (outWit == null) {
				outWit = getWorkItemType(collection, project, witTemp.referenceName, 'layout', 'System.CurrentProcessTemplateId')
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
		
		// Add fields from default WIT since there are certain fields needed by our back-end processes
		defaultWitFields.value.each { field ->
			addWITField(collection, project, actualWit.referenceName, field.referenceName, field.type)
		}
		ensureLayout(collection, project, actualWit, defaultWit)
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
	
	def addWITField(collection, project, witReferenceName, refName, type) {
		boolean req = false
		def defVal = ''
		if (type == 'boolean') {
			req = true
			defVal = 'false'
		}
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def witField = [defaultValue: defVal, referenceName: refName, type: type, readOnly: false, required: req,  allowedGroups: null]
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

