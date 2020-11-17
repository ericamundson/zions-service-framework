package com.zions.vsts.services.action.mr

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
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.mr.SmartDocManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.mr.SmartDoc

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.json.JsonSlurper

/**
 * Provides command line tool to move Smart Docs from one Team Project in ADO to another.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>moveSmartDocs - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.collection - ADO organization</li>
 *  <li>tfs.collection.guid - ADO org uid</li>
 *  <li>import.file - name of Excel file with list of Smart Docs to be moved</li>
 *  <li>mr.repo.name - Modern Requirements repository with synched Smart Doc .smd files</li>
 *  <li>mr.branch - Modern Requirements repository default branch name</li>
 *  </ul>
 * </ul>
 */
@Component
@Slf4j
class MoveSmartDocs implements CliAction {
	int titleCol, numCols
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ExcelManagementService excelManagementService
	
	@Autowired
	SmartDocManagementService smartDocManagementService
	@Value('${tfs.url:}')
	String tfsUrl

	@Value('${tfs.collection:}')
	String collection	
	
	@Value('${tfs.collection.guid:}')
	String tfsCollectionGUID
	
	@Value('${mr.repo:}')
	String repoName
	
	@Value('${mr.branch:}')
	String branch
	ChangeListManager clManager
	
	public def execute(ApplicationArguments data) {
		def inFilePath = data.getOptionValues('import.file')[0]
		
		// Open input Excel doc
		if (!excelManagementService.openExcelFile(inFilePath)) return
						
		def wiChanges
		Sheet sheet = excelManagementService.getSheet0()
		boolean error = false
		boolean isFirstRow = true
        sheet.each { row -> 
			if (isFirstRow) {
				excelManagementService.setHeaders(row)
				
				// Check required columns
				numCols = excelManagementService.headers.size()
				List requiredColumns = ['Title','Team Project','Target Team Project','Target Area Path','Source Folder','Target Folder','Target Template']
				titleCol = excelManagementService.getColumn('Title')
				def missingColumns = excelManagementService.getMissingColumns(requiredColumns)
				if (missingColumns) {
					log.error("***ERROR*** Missing required column: $missingColumns")
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
				// Move next Smart Doc 
				moveSmartDoc(row)
			}
        }
//		if (clManager) clManager.flush()

		println('done')
			
		return null;
	}
	public moveSmartDoc(Row row) {
		def smartDoc
		def rowMap = excelManagementService.getRowMap(row)
		String srcProjectName = rowMap['Team Project']
		def srcProject = projectManagementService.getProject(collection, srcProjectName)
		String destProjectName = rowMap['Target Team Project']
		def destProject = projectManagementService.getProject(collection, destProjectName)
		def destAreaPath = rowMap['Target Area Path']
		def srcFolder = rowMap['Source Folder']
		def destFolder = rowMap['Target Folder']
		def destTemplate = rowMap['Target Template']
		def smartDocName = rowMap['Title']
		
		// get .smd file
		def repo = codeManagementService.getRepo(collection, srcProject, repoName)
		String fileName = "/inteGREAT/SmartDocs$srcFolder/${smartDocName}.smd"
		def smdFileContent = codeManagementService.getFileContent(collection, srcProject, repo, fileName, branch)
		if (smdFileContent) {
			def smdMetadata = new JsonSlurper().parseText(smdFileContent)
			smartDoc = new SmartDoc(smartDocName, destFolder, destTemplate, smdMetadata)
		}
		else {
			log.error("Could not retrieve .smd file for document: $smartDocName")
			return
		}
		
		// move work items, then create Smart Doc in new project
		try {
			moveWorkItems(smartDoc, destProject, destAreaPath)
			def result = smartDocManagementService.ensureSmartDoc('Create', smartDoc, tfsUrl, collection, tfsCollectionGUID, destProject)
			if (result == null) {
				log.info("SmartDoc API returned null")
			}
			else if (result.error != null && result.error.code != "null") {
				log.info("SmartDoc API failed.  Error code: ${result.error.code}, Error message: ${result.error.message}, Error name: ${result.error.name}")
			}
			else {
				log.info("SmartDoc API succeeded. Result: ${result.result}")
			}

		}
		catch (e) {
			log.error("An exception occurred trying to move ${smartDocName}: ${e.message}")
		}
	}
	private moveWorkItems(SmartDoc smartDoc, def destProject, String destAreaPath) {
		clManager = new ChangeListManager(collection, workManagementService )
		smartDoc.workItems.each { id ->
			def wiChange = getChanges(id, destProject, destAreaPath)
			if (wiChange) {
				clManager.add("${id}", wiChange)
			}
		}
		clManager.flush()
	}
	public getChanges(def id, def destProject, String destAreaPath) {
		def wi = workManagementService.getWorkItem(collection, destProject.name, id)
		
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${id}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]

		// Add fields	
		wiData.body.add([ op: 'add', path: "/fields/System.WorkItemType", value: "${wi.fields.'System.WorkItemType'}"])			
		wiData.body.add([ op: 'add', path: "/fields/System.Title", value: "${wi.fields.'System.Title'}"])
		wiData.body.add([ op: 'add', path: "/fields/System.State", value: "${wi.fields.'System.State'}"])
		wiData.body.add([ op: 'add', path: "/fields/System.TeamProject", value: "${destProject.name}"])
		wiData.body.add([ op: 'add', path: "/fields/System.AreaPath", value: "${destAreaPath}"])
		wiData.body.add([ op: 'add', path: "/fields/System.IterationPath", value: "${destProject.name}"])
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


}
