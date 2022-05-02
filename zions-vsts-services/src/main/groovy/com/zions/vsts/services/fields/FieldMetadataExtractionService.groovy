package com.zions.vsts.services.fields

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.excel.ExcelManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.extdata.ExtensionDataManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Extracts all field metadata for a collection and generates Field Report in Excel.
 * 
 * @author Robert Huet
 */
@Component
@Slf4j
class FieldMetadataExtractionService {
	@Autowired
	ProcessTemplateService processTemplateService;
	
	@Autowired
	ProjectManagementService projectManagementService
		
	@Autowired
	ExtensionDataManagementService extensionDataManagementService
	
	@Autowired
	ExcelManagementService excelManagementService
		
	List<String> witFields = []
	List<String> projectNames = []

	public def exportFields(collection, project, witNames, outDir) {

		// get all projects for retrieving Custom Attributes control fields for each project
		def projects = projectManagementService.getProjects(collection)
		projects.each {proj -> 
			projectNames.add("${proj.name}".toString()) 
		}

		def witExportList = []
		boolean validationError = false
		def wits = processTemplateService.getWorkItemTypes(collection, project)

		// If "all" requested, then get all WITs for the process template
		if (witNames.length == 1 && witNames[0].toLowerCase() == 'all') {
			wits.value.each { wit ->
				if (!wit.isDisabled)
					witExportList.add(wit.name)
			}
		}
		// Else, make sure that all of the requested WITs exist in the source collection
		else {
			witNames.each { name ->
				def foundWit = wits.value.find { wit ->
					"${wit.name}" == "$name"
				}
				if ( foundWit) {
					witExportList.add(name)
				} else {
					log.error("ERROR:  WIT <$name> is not found in source collection $collection.")
					validationError = true
				}
			}
		}
		
		// Extract and export all requested WITs
		if (!validationError) {
			def index = 0
			def count = witExportList.size()
			excelManagementService.CreateExcelFile(outDir,"Field Report")
			witExportList.each { name ->
				def witChanges = processTemplateService.extractWitMetadata(collection, project, "${name}", ++index, count)
				insertWitIntoExcel(name, witChanges)				
			}
		
			// Output any fields that are not used by any wits
			if (witNames[0].toLowerCase() == 'all')
				insertUnusedFieldsIntoExcel()
			
			excelManagementService.CloseExcelFile()

		}
		return null;
	}
	
	def insertWitIntoExcel(String witName, def witChanges) {
		//each attribute must be inserted into the workbook without making it too messy
		//perhaps ExcelManager can add columns to current row based on insertion by header
		//log.debug("wi made it to insert method")
		//log.debug("'ID': ${wi.ID}, 'wi Type': ${wi.wiType}")
		
		witChanges.ensureFields.each { field ->
			def outfields = []
			def outfieldNdx = []
			def refName = "${field.refName}"
			if (!field.control) {
				outfields.add(field)
			}
			// Need to capture fields associated with controls	
			else if (field.associatedField) {
				def outfield = ['page': field.page, 
							    'group': field.group,
								'label': field.label, 
								'visible': field.control.visible, 
								'required': field.required,
								'name': field.associatedField.name, 
								'refName': field.associatedField.refName, 
								'type': field.associatedField.type, 
								'isPicklistSuggested': field.associatedField.isPicklistSuggested,
								'suggestedValues': field.associatedField.suggestedValues,
								'control': field.control.contribution.contributionId,
								'description': field.associatedField.description]
				outfields.add(outfield)
				
				// For CustomAttributes control, we need to get associated fields for each project and add them here as well
				if ("${outfield.refName}" == 'Custom.CustomAttributes' &&
					(witName == 'Test Plan' || witName == 'Test Suite' || witName == 'Test Case')) {
					// Get WIT Fields
					// Retrieve control for each project
					projectNames.each { projectName ->
						String key = "${witName}_Custom.CustomAttributes_$projectName"
						def extData = extensionDataManagementService.getExtensionData(key)
						if (extData) {
							extData.descriptors.each { extField ->
								outfield = ['page': field.page, 
										    'group': field.group,
											'label': extField.displayName,
											'visible': field.control.visible, 
											'required': field.required,
											'name': extField.name,
											'refName': extField.fieldName, 
											'type': extField.attributeType,
											'isPicklistSuggested': extField.isPicklistSuggested,
											'suggestedValues': extField.enumValues,
											'control': field.control.contribution.contributionId,
											'description': "",
											'project': projectName]
								outfields.add(outfield)
								outfieldNdx.add("${extField.fieldName}".toString())
							}
						}
					}
				}
			}

			if (outfields.size() > 0) {
				try {
					outfields.forEach { outfield ->
						excelManagementService.InsertNewRow(['WIT': witName, 
															 'Tab': outfield.page, 
															 'Group': outfield.group,
															 'Label': outfield.label, 
															 'Visible': outfield.visible,
															 'Required': outfield.required, 
															 'Field': outfield.name, 
															 'RefName': outfield.refName, 
															 'Type': outfield.type, 
															 'isPicklistSuggested': outfield.isPicklistSuggested,
															 'SuggestedValues': "${outfield.suggestedValues == null ? '' : outfield.suggestedValues}",
															 'Control': shortControlName(outfield.control), 
															 'Description': outfield.description,
															 'Project': outfield.project])
						if (!witFields.contains(outfield.refName)) 
							witFields.add("${outfield.refName}".toString())
					
					}
				} catch (Exception e){
					log.error("Artifact $witName had error writing to spreadsheet: ${e}")
				}
			}
		}
		
	}
	def insertUnusedFieldsIntoExcel() {
		//each attribute must be inserted into the workbook without making it too messy
		//perhaps ExcelManager can add columns to current row based on insertion by header
		//log.debug("wi made it to insert method")
		//log.debug("'ID': ${wi.ID}, 'wi Type': ${wi.wiType}")
		def allFields = processTemplateService.getFields(collection, project)
		allFields.value.each { field ->
			String refName = "${field.referenceName}".toString()
			String prefix = refName.substring(0,6)
			if (prefix == 'Custom' && !witFields.contains(refName)) {
				try {
					excelManagementService.InsertNewRow(['Field': "${field.name}", 'RefName': refName, 'Type': "$field.type", 'Description': "${field.description}"])
				} catch (Exception e){
					log.error("Field $refName had error writing to spreadsheet: ${e}")
				}
			}
		}
		
	}
	
	private String shortControlName(contributionId) {
		String controlName
		if (contributionId && contributionId != '') {
			def ndx = "$contributionId}".lastIndexOf('.')
			if (ndx > 0)
				controlName = "$contributionId}".substring(ndx+1, "$contributionId}".length() - 1)
			else
				controlName = "$contributionId}"
		}
		return controlName
	}
}
