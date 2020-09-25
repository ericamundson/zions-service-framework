package com.zions.vsts.services.action.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.*

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService
import com.zions.common.services.excel.ExcelManagementService
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.work.ChangeListManager

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.json.JsonSlurper

@Component
@Slf4j
class UpdateWorkItems implements CliAction {
	int idCol, workItemTypeCol, titleCol, priorityCol, sevCol, colorCol, numCols
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	ExcelManagementService excelManagementService
	@Autowired
	SharedAssetService sharedAssetService

	@Value('${field.map}')
	String fieldMap
	
	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.colorMapUID:}')
	String colorMapUID	

	ChangeListManager clManager

	public def execute(ApplicationArguments data) {
		def inFilePath = data.getOptionValues('import.file')[0]

		JsonSlurper js = new JsonSlurper()
		def map = js.parseText(fieldMap)
		
		// Open input Excel doc
		if (!excelManagementService.openExcelFile(inFilePath)) return
				
		clManager = new ChangeListManager(collection, workManagementService )
		
		def wiChanges
		Sheet sheet = excelManagementService.getSheet0()
		boolean error = false
		boolean isFirstRow = true
        sheet.each { row -> 
			if (isFirstRow) {
				excelManagementService.setHeaders(row)
				
				// Check required columns
				numCols = excelManagementService.headers.size()
				idCol = excelManagementService.getColumn('ID')
				workItemTypeCol = excelManagementService.getColumn('Work Item Type')
				titleCol = excelManagementService.getColumn('Title')
				priorityCol = excelManagementService.getColumn('Priority')
				sevCol = excelManagementService.getColumn('Severity')
				colorCol = excelManagementService.getColumn('Color')
				// ID, Type and Title are required by ADO
				if (idCol > numCols) {
					log.error('Missing required column: "ID"')
					error = true
				}
				else if (workItemTypeCol > numCols) {
					log.error('Missing required column: "Work Item Type"')
					error = true
				}
				else if (titleCol > numCols) {
					log.error('Missing required column: "Title"')
					error = true
				}
				// If Priority, Severity or Color are provided, then all 3 are required
				else if (priorityCol < numCols || sevCol < numCols || colorCol < numCols) {
					if (priorityCol > numCols || sevCol > numCols || colorCol > numCols) {
						log.error('Missing required columns: "Priority", "Severity" and "Color" must be included as a set.  Color will be calculated.')
						error = true
					}
				}
				isFirstRow = false
			}
			else if (!error) {
				// Check for null cell (end of file)
				if (!row.getCell(0)) {
					error = true
					return
				}
				// Display and process next work item  
				def id = excelManagementService.getCellValue(row,idCol)
				def type = excelManagementService.getCellValue(row,workItemTypeCol)
				def title = excelManagementService.getCellValue(row,titleCol)
				println("Work Itm ID: $id, Work Item Type: $type, Title: $title")
				processWorkItemChanges(id, map, row)
			}
        }
		if (clManager) clManager.flush()

		println('done')
			
		return null;
	}
	public processWorkItemChanges(def id, def map, Row row) {
		def wiChange = getChange(id, map, row)
		if (wiChange) {
			clManager.add("${id}", wiChange)
			//log.debug("adding changes for requirement ${id}")
		}
	}
	public getChange(def id, def map, Row row) {
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${id}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]

		// Add fields
		for (int i = 0; i < numCols; i++ ) {
			def fieldName = excelManagementService.headers.find { it.value == i+1 }?.key
			if (fieldName != 'ID') {
				def adoFieldName = map[fieldName]
				if (adoFieldName == 'null' || adoFieldName == null) {
					log.info("Warning: Field $fieldName has no map entry and will not be included in update")
				}
				else {
					def value
					if (fieldName == 'Color') {
						value = getColor(row)
					}
					else
						value = excelManagementService.getCellValue(row, i+1)
					if (value) {
						def idData = [ op: 'add', path: "/fields/$adoFieldName", value: "$value"]
						wiData.body.add(idData)			
					}	
				}
			}
		}
		return wiData
	}
	public getColor(Row row) {
		def priority = excelManagementService.getCellValue(row,priorityCol)
		def severity = excelManagementService.getCellValue(row,sevCol)
		def colorMap = sharedAssetService.getAsset(collection, colorMapUID)
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return colorElement.Color
	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.collection', 'import.file']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
	public getProjectFromAreaPath(String areaPath) {
		int backSlashNdx = areaPath.indexOf('\\')
		if (backSlashNdx > -1) 
			return areaPath.substring(0,backSlashNdx)
		else 
			return areaPath
		
	}

}
