package com.zions.vsts.services.cli.action.extract

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

@Component
@Slf4j
class ExtractCoreBusReq implements CliAction {
	@Autowired
	WorkManagementService workManagementService

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String team = data.getOptionValues('tfs.team')[0]
		def outFilePath = data.getOptionValues('export.file')[0]
		FileWriter outFile = new FileWriter(outFilePath)

		def result = workManagementService.getQueryWorkItems(collection, project, team, 'bcd91007-bde0-4102-91a7-7c966fa40f19')
		if (result && result.workItemRelations) {
			def lastDocId
			def lastDocName
			for (int i = 0; i < result.workItemRelations.size(); i += 3) { 
				String docId = "${result.workItemRelations[i].target.id}"
				if (i+2 >= result.workItemRelations.size()) {
					log.error("Data misalignment.  DocId=$docId.")
					return
				}
				String sectionId = "${result.workItemRelations[i+2].target.id}"
				def children = workManagementService.getChildren(collection, project, sectionId)
				if (children) {
					children.each { child ->
						if ("${child.fields.'System.WorkItemType'}" != 'Business Requirement') {
							if (docId != lastDocId) {
								def docResult = workManagementService.getWorkItem(collection, project, docId)
								if (docResult) {
									lastDocName = "${docResult.fields.'System.Title'}"
									lastDocId = docId
								}
								else {
									log.error("Error retrieving document title for $docId")
									return
								}
							}
							output(outFile, docId, lastDocName, "${child.fields.'System.WorkItemType'}", "${child.fields.'System.Id'}", "${child.fields.'System.Title'}")
						}
					}
				}
			}
		}
//		Close the file
		outFile.flush()
		outFile.close()
		return null;
	}
	
	private void output(FileWriter outFile, docId, docName, type, id, title) {
		outFile.println( docId + ',"' + docName + '",' + type + ',' + id + ',"' + title.replace('"',"'") + '"' )
	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.project', 'export.file']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
