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
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}

		if (includes['phases'] != null) {
			log.info('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			def components = jamaRequirementsManagementService.queryComponents()
			components.data.each { component ->
				if (component.location.parent.project) { // Only take top level components
//					String json = new JsonBuilder(component).toPrettyString()
//					println(json)
					log.info("Retrieving Module: ${component.fields.name} ...")
					def module = jamaRequirementsManagementService.getJamaDocument(component)
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
	/*							
					// Iterate through all module elements and add them to changeList
					def ubound = module.orderedArtifacts.size() - 1
					def lastSection = 0
					0.upto(ubound, {
		
						// If Heading is immediately followed by a type of artifact that should include it's title, 
						// move Heading title to following artifact and logically delete Heading artifact
						if (module.orderedArtifacts[it].getIsHeading() && 
							it < module.orderedArtifacts.size()-1 && 
							isToMergeWithHeading(module,it+1)) {
							
							module.orderedArtifacts[it+1].setTitle(module.orderedArtifacts[it].getTitle())
							module.orderedArtifacts[it+1].setDepth(module.orderedArtifacts[it].getDepth())
							module.orderedArtifacts[it].setIsDeleted(true)
							return  // Skip Heading artifact 
						}
						// If simple heading, remove duplicate description
						else if (module.orderedArtifacts[it].getIsHeading()) {
							module.orderedArtifacts[it].setDescription('') 
						}
						// If Reporting Requirement is in Reporting RRZ (or included in RSZ), do not migrate the artifact
						else if ((module.getArtifactType() == 'Reporting RRZ' || module.getArtifactType() == 'RSZ Specification') && 
								  module.orderedArtifacts[it].getFormat() == 'Text' &&
								 (module.orderedArtifacts[it].getArtifactType() == 'Reporting Requirement'||
								  module.orderedArtifacts[it].getArtifactType() == 'Reporting RRZ')) {
							module.orderedArtifacts[it].setIsDeleted(true)
						}
						// Do not migrate Subtopic artifacts
						else if (module.orderedArtifacts[it].getArtifactType() == 'Subtopic') {
							module.orderedArtifacts[it].setIsMigrating(false)
						}
	
						// Only store first occurrence of an artifact in the module
						if (!module.orderedArtifacts[it].getIsDuplicate() && module.orderedArtifacts[it].getIsMigrating()) {  
							changes = clmRequirementsItemManagementService.getChanges(tfsProject, module.orderedArtifacts[it], memberMap)
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
						// Adjust depth to preserve outline numbering
						if (lastSection > 0 && module.orderedArtifacts[it].getTfsWorkitemType() != 'Section' &&
							module.orderedArtifacts[it].getTfsWorkitemType() != 'Document' &&
							module.orderedArtifacts[it].getDepth() <= module.orderedArtifacts[lastSection].getDepth()){ 
							// For all requirement content under a section, increment the depth if not already incremented so as to preserve outline numbering from DOORS
							module.orderedArtifacts[it].incrementDepth(1)
						}					
						// save index of last section
						if (module.orderedArtifacts[it].getTfsWorkitemType() == 'Section') {
							lastSection = it
						}
	
					})
					
					// Create/update work items and SmartDoc container in Azure DevOps
					if (changeList.size() > 0 && errCount == 0) {
						// Process work item changes in Azure DevOps
						log.info("${getCurTimestamp()} - Processing work item changes...")
						workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
						
						// Create/update the SmartDoc
						log.info("${getCurTimestamp()} - Creating SmartDoc: ${module.getTitle()}")
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
		*/
				}
			}
			log.info("Processing completed")
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
