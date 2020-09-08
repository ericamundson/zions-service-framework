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
import com.zions.vsts.services.work.ChangeListManager

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.json.JsonSlurper

@Component
@Slf4j
class ImportWorkItems implements CliAction {
	def headers
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	ExcelManagementService excelManagementService
	@Value('${field.map}')
	String fieldMap
	ChangeListManager clManager

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		def inFilePath = data.getOptionValues('import.file')[0]

		JsonSlurper js = new JsonSlurper()
		def map = js.parseText(fieldMap)
		
		// Open input Excel doc
		if (!excelManagementService.openExcelFile(inFilePath)) return
		
		def wiChanges
		Sheet sheet = excelManagementService.getSheet0()
		int idCol
		int areaPathCol
		boolean error = false
		boolean isFirstRow = true
		def lastProject
        sheet.each { row -> 
			if (isFirstRow) {
				excelManagementService.setHeaders(row)
				
				// Check required columns
				int numCols = excelManagementService.headers.size()
				idCol = excelManagementService.getColumn('ID')
				areaPathCol = excelManagementService.getColumn('Area Path')
				if (idCol > numCols) {
					log.error('Missing required column: "ID"')
					error = true
				}
				else if (areaPathCol > numCols) {
					log.error('Missing required column: "Area Path"')
					error = true
				}


				isFirstRow = false
			}
			else if (!error) {
				// Get project from Area Path
				def id = excelManagementService.getCellValue(row,idCol)
				def tfsProject = getProjectFromAreaPath(excelManagementService.getCellValue(row,areaPathCol))
				println("Work Itm ID: $id, Project: $tfsProject")
				if (tfsProject != lastProject) {
					if (clManager) clManager.flush()
					clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					lastProject = tfsProject
				}
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
		for (int i = 0; i < excelManagementService.headers.size();i++ ) {
			def fieldName = row.cells[i]
			if (fieldName != 'ID') {
				def adoFieldName = map[fieldName]
				def idData = [ op: 'add', path: "/fields/$adoFieldName", value: "$fieldName"]
				wiData.body.add(idData)
			}
		}
		return wiData
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
			return areaPath.substring(0,backSlashNdx-1)
		else 
			return areaPath
		
	}

}
