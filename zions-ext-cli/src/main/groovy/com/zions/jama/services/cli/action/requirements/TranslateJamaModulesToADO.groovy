package com.zions.jama.services.cli.action.requirements

import java.util.Map
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.jama.services.requirements.JamaRequirementsManagementService
import com.zions.jama.services.requirements.JamaRequirementsFileManagementService
import com.zions.jama.services.requirements.JamaRequirementsItemManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.mr.SmartDocManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import com.zions.vsts.services.work.planning.AreaPathManagementService
import com.zions.rm.services.requirements.ClmArtifact
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService

/**
 * Provides command line interaction to synchronize Jama requirements management with ADO.
 * 
 */
@Component
@Slf4j
class TranslateJamaModulesToADO implements CliAction {
//	@Autowired
//	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired
	JamaRequirementsManagementService jamaRequirementsManagementService;
	@Autowired
	JamaRequirementsItemManagementService jamaRequirementsItemManagementService;
	@Autowired
	JamaRequirementsFileManagementService jamaFileManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	SmartDocManagementService smartDocManagementService
	@Autowired
	AreaPathManagementService areaPathManagementService
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	@Value('${spring.data.mongodb.database}')
	String mongodb_name
	
	String jamaBaseline = '0'  // Disable parameter for now.  Might need later.
	
	public TranslateJamaModulesToADO() {
	}

	public def execute(ApplicationArguments data) {
		boolean excludeMetaUpdate = true
		def includes = [:]
		try {
			String includeList = data.getOptionValues('include.update')[0]
			def includeItems = includeList.split(',')
			includeItems.each { item ->
				includes[item] = item
			}
		} catch (e) {}
		String tfsProjectURI = data.getOptionValues('tfs.projectUri')[0]
		String tfsProject = data.getOptionValues('tfs.project')[0]
		String tfsUrl = data.getOptionValues('tfs.url')[0]
		String projectInputFile = data.getOptionValues('jama.inputFile')[0]
		String mrTemplate = data.getOptionValues('mr.template')[0]
		String mrFolder = data.getOptionValues('mr.folder')[0]
		String tfsTeamGUID = data.getOptionValues('tfs.teamGuid')[0]
		String tfsCollectionGUID = data.getOptionValues('tfs.collectionId')[0]
		String tfsAreaPathBase = data.getOptionValues('tfs.areapath')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}

		if (includes['phases'] != null) {
			log.info('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			def baseline 

			// Get list of project IDs to migrate
			String fileContents = new File(projectInputFile).text
			def projectList = fileContents.split(',')
			int projectCounter = 0
			projectList.each { projectID -> 
				projectCounter++
				def projectFile = new File(getProjectMarkerFilename(projectID))
				if (projectFile.exists()) {
					log.info("Skipping $projectID")
					return
				}
				jamaRequirementsManagementService.jamaProjectID = projectID
				int count = 0
				def changeList = []
				def idMap = [:]
				def project = jamaRequirementsManagementService.queryProjectData()
				
				log.info("Retrieving project $projectCounter of ${projectList.size()}: ${project.fields.name} ...")
	
				def module = jamaRequirementsManagementService.getDocument(baseline, project)
				
				// If this project has no items, then go to the next
				if (module.orderedArtifacts.size() == 0) {
					log.info("***No items for module: ${module.getTitle()}(project ID: $projectID). Will not migrate.")
					return
				}
				
				// Create area path for this project and set it for all artifacts
				String moduleTitle = "${module.getTitle().replace('&','-').trim()}"
				def areaPath = "\\${tfsProject}\\${tfsAreaPathBase}\\$moduleTitle"
				if (!areaPathManagementService.createAreaPath(collection, tfsProject, tfsAreaPathBase, moduleTitle)) {
					log.info("Warning: Area Path $areaPath already exists") }
				ClmArtifact.areaPath = areaPath

									// Process attachments
				if (module.attachments.size() > 0) {
					log.info("Uploading ${module.attachments.size()} attachments for module: ${module.getTitle()} ...")
					jamaFileManagementService.ensureMultipleFileAttachments(module, module.attachments)
				}
				module.orderedArtifacts.each { item ->
					if (item.attachments.size() > 0) {
						jamaFileManagementService.ensureMultipleFileAttachments(item, item.attachments)
					}
				}
				
				log.info("Processing and caching module: ${module.getTitle()} ...")
				// Add Module artifact to ChangeList (to create Document)
				def changes = jamaRequirementsItemManagementService.getChanges(tfsProject, module, memberMap)
				def aid = module.getCacheID()
				changes.each { key, val ->
					String idkey = "${aid}"
					idMap[count] = idkey
					changeList.add(val)
					count++		
				}
							
				// Iterate through all module elements and add them to changeList
				def ubound = module.orderedArtifacts.size() - 1
				def lastSection = 0
				0.upto(ubound, {	
					// Only store first occurrence of an artifact in the module
					if (!module.orderedArtifacts[it].getIsDuplicate() && module.orderedArtifacts[it].getIsMigrating()) {  
						changes = jamaRequirementsItemManagementService.getChanges(tfsProject, module.orderedArtifacts[it], memberMap)
						aid = module.orderedArtifacts[it].getCacheID()
						changes.each { key, val ->
							String idkey = "${aid}"
							idMap[count] = idkey
							changeList.add(val)
							count++
						}
					}
					else {
						log.info("Skipping duplicate requirement #${module.orderedArtifacts[it].getID()}")
					}
					// save index of last section
					if (module.orderedArtifacts[it].getTfsWorkitemType() == 'Section') {
						lastSection = it
					}
	
				})
				
				
				// Create/update work items and SmartDoc container in Azure DevOps
				if (changeList.size() > 0) {
					// Process work item changes in Azure DevOps
					log.info("Updating ADO with work item changes...")
					workManagementService.batchWIChanges(collection, changeList, idMap)
					
					
					// Create/update the SmartDoc
					log.info("Creating SmartDoc: ${module.getTitle()}")
					def result = smartDocManagementService.ensureSmartDoc(module, tfsUrl, collection, tfsCollectionGUID, tfsProject, tfsProjectURI, tfsTeamGUID, mrTemplate, mrFolder)
					if (result == null) {
						log.info("SmartDoc API returned null")
					}
					else if (result.error != null && result.error.code != "null") {
						log.info("SmartDoc API failed.  Error code: ${result.error.code}, Error message: ${result.error.message}, Error name: ${result.error.name}")
					}
					else {
						log.info("SmartDoc API succeeded. Result: ${result.result}")
						createFileMarker(projectID)
					}
	
				}
			}
			log.info("Project migration completed")
		}

		if (includes['linkViaCache']) {
			log.info('Establishing work item links for all migrated projects...')
			int page = 0
			ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
			while (true) {
				def linkinfos = cacheManagementService.getAllOfType('LinkInfo', page)
				if (linkinfos.size() == 0) break
				linkinfos.each { link ->
					int id = Integer.parseInt("${link.key}")
					jamaRequirementsItemManagementService.getWILinkChanges(id, tfsProject) { key, changes ->
						if (key == 'WorkItem') {
							clManager.add("${id}", changes)
						}
					}
				}
				if (clManager.size() > 0) {
					log.info("Flushing ${clManager.size()} links from page ${page}")
					clManager.flush()
				}
				page++
			}
		}
			
	}

	
	def createFileMarker(def projectID) {
       File file = new File(getProjectMarkerFilename(projectID))
	   file << 'success'
	}
	
	def getProjectMarkerFilename(def projectID) {
		return "C:/$mongodb_name/$projectID" + ".txt"
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.user', 'tfs.projectUri', 'tfs.url', 'tfs.collection', 'mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
