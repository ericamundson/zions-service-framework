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
	
	@Autowired(required=false)
	ExcelManagementService excelManagementService
		
	List<String> witFields = []
	List<String> projectNames = []
	
	String collection
	String project
	boolean includeUnusedFields

	public def intializeReport(String collection, String project, String outDir, boolean includeUnusedFields) {
		this.collection = collection
		this.project = project
		this.includeUnusedFields = includeUnusedFields
		
		// get all projects for retrieving Custom Attributes control fields for each project
		def projects = projectManagementService.getProjects(collection)
		projects.each {proj -> 
			projectNames.add("${proj.name}".toString()) 
		}
		
		excelManagementService.CreateExcelFile(outDir,"Field Report")
	}
	
	public def addWitToReport(String witName, def witChanges) {
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
	
	public def closeReport()	{	
		// Output any fields that are not used by any wits
		if (includeUnusedFields)
			insertUnusedFieldsIntoExcel()
		
		excelManagementService.CloseExcelFile()
	}
	
	private def insertUnusedFieldsIntoExcel() {
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
