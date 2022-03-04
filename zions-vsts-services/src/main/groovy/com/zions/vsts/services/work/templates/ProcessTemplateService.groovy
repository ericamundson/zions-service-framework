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
	private sourceCollection 
	private sourceProject
	private def collectionFields = [:]
	
	private def typeMap = [:]
	private def nameMap = [:]
	
	
    public ProcessTemplateService() {
	}
	// The following public methods are the main entry points for extracting and updating (synching) WIT metadata
	// Extracts all WIT metadata from a source ADO org
	public def extractWitMetadata(collection, project, String wiName, def index, def count) {
		// Retain source collection and project to support synching to a target collection
		sourceCollection = collection
		sourceProject = project
		
		log.info(">>>>>>>>>> $index of $count: Retrieving <$wiName> metadata from $collection...")
		
		// Get WIT general properties
		def wit = getWIT(collection, project, wiName)

		// Initialize the structure that will hold all the WIT data
		def witChanges = [ensureType: wiName, ensureFields: [], ensureRules: [], ensureStates: []]
		
		// Extract WIT rules
		witChanges.ensureRules = getWITRules(collection, project, wit)
		
		// Extract WIT states
		witChanges.ensureStates = getWITStates(collection, project, wit)
		
		// Extract all WIT fields/controls
		def fieldMap = [:]
		def witFields = getWITFields(collection, project, wiName)
		witFields.'value'.each { field ->
			def fieldDetails = getField(collection, project, field.referenceName)
			String type = 'string'
			if (!fieldDetails) {
				log.error("Could ot get field details for ${field.referenceName}")
				return
			}
			type = getFieldType(fieldDetails)
			def cField = [name: "${field.name}", label: "${fieldDetails.label}", refName:"${field.referenceName}",
						type: type, description: "${fieldDetails.description}", page: null, section: null, group: null,suggestedValues:[]]
			field.allowedValues.each { value ->
				if (type == "integer")
					cField.suggestedValues.add(value.toInteger())
				else
					cField.suggestedValues.add(value)
			}
			fieldMap["${field.referenceName}"] = cField
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
							cfield.visible = "${control.visible}"
							cfield.readOnly = "${control.readOnly}"
						} else {
							cfield = [name: "${control.label}", label: null, refName:"${control.id}",
									visible:"${control.visible}", readOnly:"${control.readOnly}", type: '',
									description: 'custom control', page: page.label, section: section.id,
									group: group.label, control: control]
							if (control.contribution && control.contribution.contributionId) {
								fieldMap["${control.id}"] = cfield
//								println control.id
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
	
	// Ensure the creation/update of WIT metadata changes
	public def ensureWITChanges(def collection , def project, def changes, boolean updateLayout = false) {
		changes.each { witChange ->
			def witName = witChange.ensureType
			log.info("<<<<<<<<<< Applying <$witName> metadata to $collection...")
			
			// Make sure the WIT exists.  If not, create it.
			def wit = ensureWit(collection, project, witName)
			
			// Apply any changes to fields/controls
			witChange.ensureFields.each { witFieldChange ->
				ensureWitField(collection, project, wit, witFieldChange, updateLayout)
			}
			
			// Now, make sure the states are in sync (this will impact rules)
			ensureWITStates(collection, project, wit, witChange.ensureStates)
						
			// Finally, make sure the rules are up to date
			ensureWITRules(collection, project, wit, witChange.ensureRules)

			
		}
	}
	
	private def ensureWitField(collection, project, wit, witFieldChange, boolean updateLayout = false) {
		String refName = "${witFieldChange.refName}"
		boolean isNewField
		if (witFieldChange.control) {
			def layout = ensureWitFieldLayout(collection, project, wit, null, witFieldChange)
			return
		}
		def field = queryForField(collection, project, witFieldChange.refName)
		if (field == null) {  // New field
			isNewField = true
			def pickList = null
			if (witFieldChange.suggestedValues.size() > 0) {
				log.info("Creating picklist for field ${witFieldChange.refName}")
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
		def witField
		if (!isNewField) witField = getWITField(collection, project, wit.referenceName, field.referenceName)
		if (witField == null || "${field.referenceName}" != "${witField.referenceName}") {
			witField = addWITField(collection, project, wit.referenceName, field.referenceName, field.type)
		}
		if (witField != null && updateLayout) {
			def layout = ensureWitFieldLayout(collection, project, wit, field, witFieldChange)
		}
		
	}
	
	private def ensureWITRules(collection, project, wit, ruleChanges) {
		// If rules don't match, do complete refresh (update api does not work)
		def existingRules = getWITRules(collection, project, wit)
		if (!rulesCompare(existingRules, ruleChanges)) {
			log.info("Refreshing WIT rules")
			existingRules.each { existingRule ->
				deleteWITRule(collection, project, wit, existingRule.id)
			}
			
			// Create new rules
			ruleChanges.each() { ruleChange ->
				addWITRule(collection, project, wit, ruleChange)
			}
		}
	}
	
	private boolean rulesCompare(existingRules, ruleChanges) {
		if (existingRules.size() != ruleChanges.size())
			return false		
			
		boolean match = true
		ruleChanges.each() { ruleChange ->
			if (!match) return
			// See if the rule exists
			def existingRule = existingRules.find { rule ->
				"${rule.name}" == "${ruleChange.name}"
			}
			// If rule does not exist, no match
			if (!existingRule)
				match = false

			// If rule exists, and data is different, then no match
			else if ((existingRule.actions.toString()!= ruleChange.actions.toString()) ||
					 (existingRule.conditions.toString()!= ruleChange.conditions.toString()) ||
					 (existingRule.isDisabled != ruleChange.isDisabled))
				match = false

		}
		return match
	}
	
	private def ensureWITStates(collection, project, wit, stateChanges) {
		// Make sure every source state exists
		def existingStates = getWITStates(collection, project, wit)
		def newStates = []
		stateChanges.each { stateChange ->
			def foundState = existingStates.find { existingState ->
				"${stateChange.name}" == "${existingState.name}"
			}
			if (!foundState) {
				// Create new state
				addWITState(collection, project, wit, stateChange)
			}
		}
	}
	
	private def getFieldType(field) {
		def type = "${field.type}".trim()
		if (type == 'string' && field.isIdentity == true) {
			type = 'identity'
		}
		return type
	}
	
	def getWorkItemTypes(String collection, String project, expand = 'none', projectProperty = 'System.ProcessTemplateType') {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, projectProperty)
		def aproject = encode(project)
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
		def aproject = encode(project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes/${refName}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand':expand]
			)

		return result;

	}

	public def getWITFields(String collection, String project,  String workItemName) {
		def projectData = projectManagementService.getProject(collection, project)
//		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def aworkItemName = encode(workItemName)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/workitemtypes/${aworkItemName}/fields",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.3', '$expand': 'all']
			)
		return result;
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
	
	def queryForField(def collection, def project, def refName) {
		if (collectionFields[collection] == null) {
			getFields(collection, project)
		}
		def field = collectionFields[collection].value.find { field ->
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
		collectionFields[collection] = result // Cache collection fields
		return result
	}

	def getField(def collection, def project, def refName) {
		def projectData = projectManagementService.getProject(collection, project)
		def eRefName = encode("$refName")
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/fields/${eRefName}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand': 'all']
			)
		return result
	}

	public def updateWorkitemTemplate(String collection, String project,  String workItemName, String body) {
		def aproject = encode(project)
		def aworkItemName = encode(workItemName)
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${aproject}/_apis/wit/workItemTypes/${aworkItemName}",
			body: body,
			headers: [Accept: 'application/json']
			)
		return result;
	}


	// Make sure import field type matches existing field type in target org
	def checkTypeMatch(def witFieldChange, def field) {
		if ("${witFieldChange.refName}".substring(0,6) != 'Custom')
			return true // don't worry about system fields
		else if ("${witFieldChange.type}" == "${field.type}" &&
				(witFieldChange.suggestedValues == [] && field.isPicklist == false) ||
				(witFieldChange.suggestedValues != [] && field.isPicklist == true))
			return true
		else if ("${witFieldChange.type}" == 'identity' && field.isIdentity == true)
			return true
		else
			return false
	}
	
	def ensureWitFieldLayout(collection, project, wit, field, witFieldChange) {
		// Check for null label or description
		if (witFieldChange.label == 'null') witFieldChange.label = witFieldChange.name
		if (witFieldChange.description == 'null') witFieldChange.description = ''
		
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
		// Check if field or external control already exists in the layout
		def control = group.controls.find { control ->
			"${control.id}" == "${witFieldChange.refName}"
		}

		if (control == null) {
		// Control does not currently exist.  It must be added
			// Check if this is an external control with associated field
			def associatedFieldName
			if (witFieldChange.control && witFieldChange.control.contribution && witFieldChange.control.contribution.inputs)
				associatedFieldName = witFieldChange.control.contribution.inputs.FieldName
			if (associatedFieldName) { // External control with associated field
				// Make sure the associated field name exists in the collection
				boolean isNewField = false
				def foundField = queryForField(collection,project,associatedFieldName)
				if (!foundField && sourceCollection) {
					// Must get field from source collection
					foundField = queryForField(sourceCollection, sourceProject, associatedFieldName)
					if (foundField) {
						isNewField = true
						def type = getFieldType(foundField)
						def wiField = [refName: foundField.referenceName, name: foundField.name, type: type, description: foundField.description]
						def pickList
						if (foundField.isPicklist) {
							// Retrieve picklist values from picklistID
							def picklist = getPickList(sourceCollection, sourceProject, foundField.picklistId)
							def picklistChanges = [type: picklist.type, suggestedValues: []]
							picklist.items.each { val ->
								picklistChanges.suggestedValues.add(val)
							}
							log.info("Creating picklist for field ${wiField.referenceName}")
							pickList = createPickList(collection, project, picklistChanges)
						}
						createField(collection, project, wiField, pickList)
					}
				}
				
				// Next, make sure this field has been added to the WIT
				def witField
				if (!isNewField) witField = getWITField(collection, project, wit.referenceName, associatedFieldName)
				if (witField == null) {
					witField = addWITField(collection, project, wit.referenceName, associatedFieldName, getFieldType(foundField))
				}
				
			}
			// Make sure the external control exists on the WIT layout
			ensureExternalControl(collection, project, wit, group, witFieldChange, genericRestClient.&post)
		}
		else {
			boolean updateRequired = ("${control.label}" != "${witFieldChange.label}") ||
									("${control.visible}" != "${witFieldChange.visible}") ||
									("${control.readOnly}" != "${witFieldChange.readOnly}")  // more conditions to be added
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
			controlData = [order:null, label:field.label, id: field.refName, readOnly: field.readOnly, visible:field.visible, isContribution: false, controlType:null, metadata:null, inherited:null, overridden:null, watermark:null, height:null]
		}
		
		String eControlId = encode("${controlData.id}")
		String groupId = encode("${externalGroup.id}")
		def body = new JsonBuilder(controlData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = operation(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workItemTypes/${wit.referenceName}/layout/groups/${groupId}/Controls/${eControlId}",
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
		log.info("Creating new group $name")
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		String pageId = encode("${externalPage.id}")
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
		String pageId = encode("${externalPage.id}")
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
		log.info("Creating new page $name")
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
		log.info("Creating field ${witFieldChange.refName}")
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def pickId = null
		def wiData = [id: "${witFieldChange.refName}", name: "${witFieldChange.name}", type: "${witFieldChange.type}", description: "${witFieldChange.description}", pickList: null]
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
		if (result) // If new field is created, flush the fields cache
			collectionFields[collection] = null
			
		return result
	}
	def updateField(collection, project, witFieldChange, pickList, field) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${witFieldChange.refName}")
		def pickId = null
		def wiData = [id: "${field.referenceName}", name: "${field.name}", type: "${field.type}", description: "${witFieldChange.description}", pickList: null]
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
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processdefinitions/${processTemplateId}/fields/$eRefName",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.1']
			
			)
		return result
	}

	def createPickList(collection, project, witFieldChange) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def guid = UUID.randomUUID().toString()
		def listData = [id: null, name: "picklist_${guid}", type: witFieldChange.type, url: null, isSuggested: true, 
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

	private def getPickList(def collection, def project, def pickListId) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/lists/${pickListId}",
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
		log.info("Adding new group $groupName")
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
		log.info("Adding control ${controlData.id}")
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		//def controlData = [contribution: null, controls:[], height:null, id:null, inherited:null, isContribution:false, label:groupName, order:null, overridden:null, visible:true]
		controlData.inherited = null
		controlData.controlType = null
		String eControlId = encode("${controlData.id}")
		def body = new JsonBuilder(controlData).toPrettyString()
		
		//def pName = URLEncoder.encode(this.processName, 'utf-8').replace('+', '%20')
		
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workItemTypes/${wit.referenceName}/layout/groups/${groupid}/Controls/${eControlId}",
			body: body,
			headers: [accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true' 
				//referer: "${genericRestClient.getTfsUrl()}/_admin/_process?process-name=${pName}&type-id=${wit.referenceName}&_a=layout"
				]
			//query: ['api-version': '5.0-preview.1']
			
			)
		return result

	}
	
	def addWITField(collection, project, witReferenceName, refName, type) {
		log.info("Adding field ${refName} to this WIT")
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
	
	def getWITField(collection, project, witName, fieldRefname) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${fieldRefname}")
		def eWitName = encode("${witName}")
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workitemtypes/${eWitName}/fields/$eRefName",
			headers: [accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			
			)
		return result
	}
	def getWITStates(collection, project, wit) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/states",
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.1']
			)

		def customStates = []
			if (result) {
			result.value.each() { state ->
				if (state.customizationType == 'custom')
					customStates.add(state)
			}
		}
		return customStates
	}
	def getWITRules(collection, project, wit) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/rules",
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.2']
			)
		def customRules = []
		if (result) {
			result.value.each() { rule ->
				if (rule.customizationType == 'custom')
					customRules.add(rule)
			}	
		}
		return customRules
	}
	def addWITState(collection, project, wit, stateChange) {
		log.info("Adding state ${stateChange.name} to this WIT")
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")
		def wiData = [name: stateChange.name, color: stateChange.color, stateCategory: stateChange.stateCategory]
		def body = new JsonBuilder(wiData).toPrettyString()

		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/states",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.1']
			
			)
			
		return result

	}
	def addWITRule(collection, project, wit, ruleChange) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")
		def wiData = [actions: ruleChange.actions, conditions: ruleChange.conditions, isDisabled: ruleChange.isDisabled, name: ruleChange.name]
		def body = new JsonBuilder(wiData).toPrettyString()

		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/rules",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.2']
			
			)
			
		return result

	}
	def deleteWITState(collection, project, wit, stateId) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")

		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/states/$stateId",
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.1']
		)
			
		return result


	}
	def deleteWITRule(collection, project, wit, ruleId) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def eRefName = encode("${wit.referenceName}")

		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId.toString()}/workItemTypes/$eRefName/rules/$ruleId",
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.2']
		)
			
		return result


	}
	def genColor() {
		Random random = new Random();
		int nextInt = random.nextInt(256*256*256);
		return String.format("%06x", nextInt);
		
	}

	def encode (String inputString) {
		return URLEncoder.encode(inputString, 'utf-8').replace('+', '%20').replace('#', '%23')
	}
}

