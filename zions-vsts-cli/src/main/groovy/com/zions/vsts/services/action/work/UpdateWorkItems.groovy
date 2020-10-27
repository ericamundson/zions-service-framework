package com.zions.vsts.services.action.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.*

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.calculations.CalculationManagementService
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
	int idCol, workItemTypeCol, titleCol, numCols
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	ExcelManagementService excelManagementService
	
	@Autowired
	SharedAssetService sharedAssetService
	
	@Autowired
	CalculationManagementService fieldCalcManager

	@Value('${field.map}')
	String fieldMapFilename
	
	@Value('${link.map}')
	String linkMapFilename

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.colorMapUID:}')
	String colorMapUID	

	ChangeListManager clManager
	
	def fieldMap
	def linkMap

	public def execute(ApplicationArguments data) {
		def includes = [:]
		try {
			String includeList = data.getOptionValues('include.update')[0]
			def includeItems = includeList.split(',')
			includeItems.each { item ->
				includes[item] = item
			}
		} catch (e) {}

		def inFilePath = data.getOptionValues('import.file')[0]
		fieldMap = getMap(fieldMapFilename)
		linkMap = getMap(linkMapFilename)
		
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
				processWorkItemChanges(includes, id, row)
			}
        }
		if (clManager) clManager.flush()

		println('done')
			
		return null;
	}
	public processWorkItemChanges(def includes, def id, Row row) {
		def wiChange = getChanges(includes, id, row)
		
		if (wiChange) {
			clManager.add("${id}", wiChange)
			//log.debug("adding changes for requirement ${id}")
		}
	}
	
	public getChanges(def includes, def id, Row row) {
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${id}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def rowMap = excelManagementService.getRowMap(row)

		// Add fields
		if (includes['fields'] != null) {
			rowMap.each { mapEntry ->
				def fieldName = mapEntry.key
				if (fieldName != 'ID') {
					if (!fieldMap[fieldName]) {
						println("WARNING: No map entry for <$fieldName>.  This field will not be updated.")
						return
					}
					def adoFieldName = fieldMap[fieldName].AdoId
					def handler = fieldMap[fieldName].CalcHandler
					if (adoFieldName == 'null' || adoFieldName == null) {
						log.info("Warning: Field $fieldName has no map entry and will not be included in update")
					}
					else {
						def value
						if (handler)  // use calculation handler to get value
							value = fieldCalcManager.execute([ targetField: adoFieldName, fields: rowMap ], handler)
						else
							value = mapEntry.value
						if (!value || value == 'null') {
							value = ''
						}	
						def idData = [ op: 'add', path: "/fields/$adoFieldName", value: "$value"]
						wiData.body.add(idData)			
					}
				}
			}
		}
		if (includes['links'] != null) {
			def project = rowMap['Team Project']
			// throw error if no Team Project
			if (!project)
				throw new Exception("Team Project is required to process link updates")
			// Get work item
			def wi = workManagementService.getWorkItem(collection,project,id.toString())
			// Iterate through wi links and update any types that are in the map
			if (wi.relations) {
				def count = wi.relations.size()
				for (int i = 0; i < count; i++) {
					def linkName = "${wi.relations[i].attributes.name}"
					if (linkMap[linkName]) {
						String targetRel = linkMap[linkName]
						def url = "${wi.relations[i].url}"
						def idData = [op: 'remove', path: "/relations/$i"]
						wiData.body.add(idData)
						if (targetRel.toLowerCase() != 'delete') {
							idData = [op: 'add', path: '/relations/-', value: [rel: targetRel, url: url, attributes:[comment: "Replaces $linkName link"]]]
							wiData.body.add(idData)
						}
					}
				}
			}
		} 
		
		return wiData
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.collection', 'import.file','include.update']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
	public def getMap(String mapFilename) {
		File resource = new ClassPathResource(mapFilename).getFile()
		if (!resource) {
			println("ERROR: Could not find mapping resource file $mapFilename")
			return
		}
		return new JsonSlurper().parseText(resource.text)

	}

}
