package com.zions.jama.services.cli.action.requirements

import java.util.Map
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.jama.services.requirements.JamaRequirementsManagementService
import com.zions.jama.services.requirements.JamaRequirementsItemManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.mr.SmartDocManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
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
	WorkManagementService workManagementService
	@Autowired
	SmartDocManagementService smartDocManagementService
	
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
		String mrTemplate = data.getOptionValues('mr.template')[0]
		String mrFolder = data.getOptionValues('mr.folder')[0]
		String tfsTeamGUID = data.getOptionValues('tfs.teamGuid')[0]
		String tfsCollectionGUID = data.getOptionValues('tfs.collectionId')[0]
		String jamaBaseline = data.getOptionValues('jama.baseline')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}

		if (includes['phases'] != null) {
			log.info('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			def baselines = jamaRequirementsManagementService.queryBaselines()
			def baseline = baselines.find({it.id == jamaBaseline.toInteger() })
			if (baseline) { 
//				String json = new JsonBuilder(component).toPrettyString()
//				println(json)
				int count = 0
				def changeList = []
				def idMap = [:]
				log.info("Retrieving baseline: ${baseline.name} ...")
				def module = jamaRequirementsManagementService.getBaselineDocument(baseline)
				log.info("Processing Module: ${module.getTitle()} ...")
				
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
					log.info("Processing work item changes...")
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
					
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
					}

				}
				log.info("Processing completed")
	
			}
			else {
				log.info('Baseline not found!')
			}
		}


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
		def required = ['tfs.user', 'tfs.projectUri', 'tfs.url', 'tfs.collection', 'mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
