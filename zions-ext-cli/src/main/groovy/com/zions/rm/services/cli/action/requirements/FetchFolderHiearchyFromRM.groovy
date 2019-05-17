package com.zions.rm.services.cli.action.requirements

import java.util.Map

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.rm.services.requirements.ClmRequirementsItemManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import com.zions.rm.services.requirements.ClmArtifact


/**
* forked from TranslateRMBaseArtifactsToADO
 */
@Component
@Slf4j
class FetchFolderHiearchyFromRM implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired 
	ClmRequirementsManagementService clmRequirementsManagementService
	@Autowired 
	RequirementsMappingManagementService rmMappingManagementService
	
	public FetchFolderHiearchyFromRM() {
	}

	public def execute(ApplicationArguments data) {
		boolean excludeMetaUpdate = true
//		def includes = [:]
//		try {
//			String includeList = data.getOptionValues('include.update')[0]
//			def includeItems = includeList.split(',')
//			includeItems.each { item ->
//				includes[item] = item
//			}
//		} catch (e) {}
		String projectURI = data.getOptionValues('clm.projectAreaUri')[0]
		String mappingFile = data.getOptionValues('rm.mapping.file')[0]
		String oslcNs = data.getOptionValues('oslc.namespaces')[0]
		String oslcSelect = data.getOptionValues('oslc.select')[0]
		String oslcWhere = data.getOptionValues('oslc.where')[0]
//		String rmFilter = data.getOptionValues('rm.filter')[0]
//		String collection = ""
		String outputFile = data.getOptionValues('output.file')[0]
		def topLevelFolders = []
		//bit wordy but want to see where I break it
		try {
			String includeList = data.getOptionValues('include.folders')[0]
			topLevelFolders.addAll(includeList.split(','))
		} catch (e) {}


		println("${getCurTimestamp()} - Querying DNG Folders from list of parents ...")
		//For every input parent folder, we should get all child folders and add them to a list
		def folderUris = []
//		File mFile = new File(mappingFile)
//		if (!mFile.createNewFile()) {
//			mFile.write(""); //create or blank out file
			topLevelFolders.each { parent ->
				//DEBUG: uncomment addAll getnestedfolders and comment add(parent) to return to normal
				folderUris.addAll(getNestedFolders(parent))
//				mFile.write
				//folderUris.add(parent)
			}
			log.info("${getCurTimestamp()} - Retrieved ${folderUris.size()} folders ...")
//		} else {
//			folderUris = mFile.readObject()
//			println("${getCurTimestamp()} - Retrieved folders from cache ...")
//		}

		//def results = clmRequirementsManagementService.queryForFolders("https://clm.cs.zionsbank.com/rm/folders/_mxVp8L1REeS5FIAyBUGhBQ")
		
		//get all requirements in each folder and write a csv
		File oFile = new File(outputFile)
		oFile.createNewFile() 
		oFile.write("Id,Release\n"); //create or blank out file
		def requirementIds = []
		
		folderUris.each { folderUri ->
			log.info("Querying folder: ${folderUri}")
			def idList = getArtifactIDList(projectURI, oslcNs, oslcSelect, oslcWhere, folderUri)			
				idList.each { id ->
				oFile.append(id + ",\"Deposits\"\n")
			}
		}
			
		println("Processing completed")

	}
	
	public def getArtifactIDList(projectURI, oslcNs, oslcSelect, oslcWhere, folderUri) {
		def artifactIDs = []
		oslcWhere = oslcWhere.replace('ztargetfolder',folderUri)
		
		log.info("Querying DNG Artifacts Within Folder" )
		def results = clmRequirementsManagementService.queryForArtifacts(projectURI, oslcNs, oslcSelect, oslcWhere)
		// Continue until all pages have been processed
		log.debug("Returned from query with $artifactIDs.size()" )
		def page = 1
		while (true) {
			int count = 0
			if (results.Description.children().size() > 0 ) {
	
			results.Description.children().each { member ->
				artifactIDs.add("${member.children().identifier.text()}")
				//log.info(mem.text())
			}
			//artifactIDs = results.depthFirst.findAll{ it.name() == "identifier"}
			log.info("$artifactIDs.size() Base Artifacts were retrieved")
			
			// Process next page
			String nextUrl = "${results.ResponseInfo.nextPage.@'rdf:resource'}"
			if (nextUrl != '') {
				page++
				log.info("Retrieving page ${page}...")
				results = clmRequirementsManagementService.nextPage(nextUrl)
			}
			
			}
			return artifactIDs
			
		}
		
		log.info("Retrieved all artifacts in folder: " + folderUri)

	}
	
	//recursively get all child folders of a parent
	def getNestedFolders(String parentUri) {
		def folderUriList = []
		folderUriList.add(parentUri)
		log.info("Added folder $parentUri")
		def results = clmRequirementsManagementService.queryForFolders(parentUri)
//debug only:
		//def results = getDebugTestResults()	
//end debug
		
		log.debug(results.toString())
		//may need to assert that the results are valid/have a 200 response
//		if (results.Description.children().size() > 0) {
		results.Description.children().each { member ->
			def folderUri = member.folder.@'rdf:about'.text()
			log.debug("${getCurTimestamp()} - about to retrieve children of " + folderUri)
			//recurse this function to get children
			folderUriList.addAll(getNestedFolders(folderUri))
			log.debug("${getCurTimestamp()} - retrieved children of " + folderUri)
		}
//		} else {  }
		return folderUriList
	}
	
//	def getDebugTestResults() {
//		File sandboxFile = new File("C:\\rmfoldercache\\rmfolderreturn.xml")
//		return new XmlSlurper().parseText(sandboxFile.text)
//	}

	def getItemChanges(String project, def rmItemData, def memberMap, def whereUsed) {

		String modified = rmItemData.Requirement.modified
		String identifier = rmItemData.Requirement.identifier
		String formatString = rmItemData.Requirement.ArtifactFormat.@'rdf:resource'
		String format = formatString.substring(formatString.lastIndexOf('#') + 1)
		String about = "${rmItemData.Requirement.@'rdf:about'}"
		ClmArtifact artifact = new ClmArtifact('', format, about)
		if (format == 'Text') {
			clmRequirementsManagementService.getTextArtifact(artifact,false,false)
		}
		else if (format == 'WrapperResource'){
			clmRequirementsManagementService.getNonTextArtifact(artifact,false)
		}
		else {
			println("WARNING: Unsupported format of $format for artifact id: $identifier")
		}
		artifact.setWhereUsed(whereUsed)
		artifact.setChanges(clmRequirementsItemManagementService.getChanges(project, artifact, memberMap))
		return artifact
	}

	def getCurTimestamp() {
		new Date().format( 'yyyy/MM/dd HH:MM' )
	}
	
	def filtered(def items, String filter) {
		if (this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(items)
		}
		return items.entry.findAll { ti ->
			true
		}
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['include.folders', 'clm.url', 'clm.user', 'clm.projectAreaUri', 'clm.pageSize', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'rm.mapping.file', 'oslc.namespaces', 'oslc.select', 'oslc.where', 'rm.filter', 'mr.url','tfs.oAuthToken']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
