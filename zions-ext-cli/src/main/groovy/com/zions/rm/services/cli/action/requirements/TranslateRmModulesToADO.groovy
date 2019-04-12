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
import com.zions.vsts.services.mr.SmartDocManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService

/**
 * Provides command line interaction to synchronize RRM requirements management with ADO.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>translateRRMToADO - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - (optional) CLM password. It can be hidden in props file.</li>
 *  <li>ccm.projectArea - RQM project area</li>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO PAT</li>
 *  <li>tfs.project - ADO project</li>
 *  <li>tfs.areapath - ADO area path to set Test planning items.</li>
 *  <li>rm.mapping.file - The xml mapping file to enable field data flow.</li>
 *  <li>rm.query - The xpath RQM testplan query.</li>
 *  <li>rm.filter - the name of filter class to used to pair down items that can't be filtered by query.</li>
 *  <li>include.update - Comma delimited list of the phases that will run during execution. E.G. refresh,clean,data,execution,links,attachments</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Class design:</b></p>
 * <img src="TranslateRRMToADO_class_diagram.png"/>
 * <p><b>Behavior:</b></p>
 * <img src="TranslateRRMToADO_sequence_diagram.png"/>
 * 
 * @author z091182
 * 
 * @startuml TranslateRRMToADO_class_diagram.png
 * 
 * annotation Autowired
 * annotation Component
 * 
 * class Map<? extends String, ? extends IFilter> {
 * 	+ put(key, element)
 * 	+ get(key)
 * .. groovy access/set elements ...
 * 	+ [key] 
 * }
 * 
 * class TranslateRRMToADO {
 * ... TODO: Implement these Spring Components in zions-ext-services project...
 * @Autowired ClmRequirementsItemManagementService clmRequirementsItemManagementService
 * @Autowired ClmRequirementsManagementService clmRequirementsManagementService
 * @Autowired RequirementsMappingManagementService requirementsMappingManagementService
 * 
 * ... TODO: Need to complete implementation of ...
 * 	+validate(ApplicationArguments args)
 * 	+execute(ApplicationArguments args)
 * 	+filtered(def, String)
 * }
 * note left: @Component
 * 
 * CliAction <|-- TranslateRRMToADO
 * TranslateRRMToADO .. Autowired:  Defines class member as injected by Spring
 * TranslateRRMToADO .. Component:  Defines class as Spring Component
 * TranslateRRMToADO --> Map: @Autowired filterMap
 * TranslateRRMToADO --> MemberManagementService: @Autowired memberManagementService
 * TranslateRRMToADO --> FileManagementService: @Autowired fileManagementService
 * TranslateRRMToADO --> WorkManagementService: @Autowired workManagementService
 * @enduml
 * 
 * @startuml TranslateRRMToADO_sequence_diagram.png
 * participant CLI
 * CLI -> TranslateRRMToADO: validate arguments
 * CLI -> TranslateRRMToADO: execute
 * alt include.update has 'clean'
 * 	TranslateRRMToADO -> WorkManagementService: clean
 * end
 * alt include.update has 'data'
 *  TranslateRRMToADO -> RequirementsMappingManagementService: get field mapping
 *  TranslateRRMToADO -> MemberManagementService: get member map
 *  TranslateRRMToADO -> ClmRequirementsManagementService: get modules via query
 *  loop each { modules object structure }
 *  	TranslateRRMToADO -> ClmRequirementsItemManagementService: get changes
 *  	TranslateRRMToADO -> List: add changes
 *  end
 *  TranslateRRMToADO -> WorkManagementService: send list of changes
 * end
 * alt include.update has 'links'
 *  TranslateRRMToADO -> RequirementsMappingManagementService: get field mapping
 *  TranslateRRMToADO -> MemberManagementService: get member map
 *  TranslateRRMToADO -> ClmRequirementsManagementService: get modules via query
 *  loop each { modules object structure }
 *  	TranslateRRMToADO -> ClmRequirementsItemManagementService: get link changes
 *  	TranslateRRMToADO -> List: add changes
 *  end
 *  TranslateRRMToADO -> WorkManagementService: send list of changes
 * end
 * alt include.update has 'attachments'
 *  TranslateRRMToADO -> RequirementsMappingManagementService: get field mapping
 *  TranslateRRMToADO -> MemberManagementService: get member map
 *  TranslateRRMToADO -> ClmRequirementsManagementService: get modules via query
 *  loop each { modules object structure }
 *  	TranslateRRMToADO -> ClmRequirementsItemManagementService: get attachment changes
 *  	TranslateRRMToADO -> List: add changes
 *  end
 *  TranslateRRMToADO -> WorkManagementService: send list of changes
 * end
 * @enduml
 */
@Component
@Slf4j
class TranslateRmModulesToADO implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	SmartDocManagementService smartDocManagementService
	@Autowired 
	ClmRequirementsItemManagementService clmRequirementsItemManagementService
	@Autowired 
	ClmRequirementsManagementService clmRequirementsManagementService
	@Autowired 
	RequirementsMappingManagementService rmMappingManagementService
	@Autowired
	ClmRequirementsFileManagementService rmFileManagementService
	
	public TranslateRmModulesToADO() {
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
		String areaPath = data.getOptionValues('tfs.areapath')[0]
		String projectURI = data.getOptionValues('clm.projectAreaUri')[0]
		String tfsUser = data.getOptionValues('tfs.user')[0]
		String tfsUrl = data.getOptionValues('tfs.url')[0]
		String mappingFile = data.getOptionValues('rm.mapping.file')[0]
		String rmQuery = data.getOptionValues('rm.query')[0]
		String rmFilter = data.getOptionValues('rm.filter')[0]
		String tfsProjectURI = data.getOptionValues('tfs.projectUri')[0]
		String tfsTeamGUID = data.getOptionValues('tfs.teamGuid')[0]
		String tfsCollectionGUID = data.getOptionValues('tfs.collectionId')[0]
		String tfsAltUser = data.getOptionValues('tfs.altuser')[0]
		String tfsAltPassword = data.getOptionValues('tfs.altpassword')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		String mrTemplate = data.getOptionValues('mr.template')[0]
		String mrFolder = data.getOptionValues('mr.folder')[0]
		File mFile = new File(mappingFile)
		
		def mapping = new XmlSlurper().parseText(mFile.text)
		if (includes['whereused'] != null) {
			log.info("fetching 'where used' lookup records")
			if (clmRequirementsManagementService.queryForWhereUsed()) {
				log.info("'where used' records were retrieved")
			}
			else {
				log.error('***Error retrieving "Where Used" lookup.  Check the log for details')
			}			
		}
		//refresh.
		if (includes['phases'] != null) {
			// Get field mappings, target members map and RM modules to translate to ADO
			log.info('Getting Mapping Data...')
			def mappingData = rmMappingManagementService.mappingData
			log.info('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			log.info("${getCurTimestamp()} - Querying DNG Modules for $rmQuery ...")
			def modules = clmRequirementsManagementService.queryForModules(projectURI, rmQuery)
			def changeList = []
			def idMap = [:]
			int iModule = 1
			int count = 0
			modules.each { module ->
				log.info("${getCurTimestamp()} - Processing Module: ${module.getTitle()} (${iModule++} of ${modules.size()}) ...")
				module.checkForDuplicates()
				// Check all artifacts for "Heading"/"Row type" inconsistencies, then abort on this module if any were found
				def errCount = 0
				module.orderedArtifacts.each { artifact ->
					if ((artifact.getIsHeading() && artifact.getArtifactType() != 'Heading') ||
						(!artifact.getIsHeading() && artifact.getArtifactType() == 'Heading') ) {
						log.error("*** ERROR: Artifact #${artifact.getID()} has inconsistent heading row type in module ${module.getTitle()}")
						errCount++
					}
					else if (artifact.getIsHeading() && (artifact.hasEmbeddedImage() || artifact.getFormat() == 'WrapperResource')) {
						log.error("*** ERROR: Artifact #${artifact.getID()} is heading with image or attachment in module ${module.getTitle()}")
						errCount++
					} 
					else if (artifact.getIsDuplicate()) {
						log.error("*** ERROR: Artifact #${artifact.getID()} is a duplicate instance in module ${module.getTitle()}.  This is not yet supported in ADO.")
						errCount++
					}
				}
				if (errCount > 0) {
					log.info("*** ERROR: Skipping module '${module.getTitle()}' due to $errCount errors")
					return // goes to next module
				}
				
				// Add Module artifact to ChangeList (to create Document)
				def changes = clmRequirementsItemManagementService.getChanges(tfsProject, module, memberMap)
				def aid = module.getCacheID()
				changes.each { key, val ->
					String idkey = "${aid}"
					idMap[count] = idkey
					changeList.add(val)
					count++		
				}
							
				// Iterate through all module elements and add them to changeList
				def ubound = module.orderedArtifacts.size() - 1
				0.upto(ubound, {
	
					// If Heading is immediately followed by Supporting Material, move Heading title to Supporting Material and logically delete Heading artifact
					if (module.orderedArtifacts[it].getIsHeading() && 
						it < module.orderedArtifacts.size()-1 && 
						isToIncorporateTitle(module,it+1)) {
						
						module.orderedArtifacts[it+1].setTitle(module.orderedArtifacts[it].getTitle())
						module.orderedArtifacts[it].setIsDeleted(true)
						return  // Skip Heading artifact 
					}
					else if (module.orderedArtifacts[it].getIsHeading()) {
						module.orderedArtifacts[it].setDescription('') // If simple heading, remove duplicate description
					}
					else if (it > 0 && module.orderedArtifacts[it].getDepth() <= module.orderedArtifacts[it-1].getDepth()){ 
						// For all other content, increment the depth if not already incremented so as to preserve outline numbering from DOORS
						module.orderedArtifacts[it].incrementDepth(1)
					}
					if (!module.orderedArtifacts[it].getIsDuplicate()) {  // Only store first occurrence of an artifact in the module
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
				})
				
	
	
				// Create work items and SmartDoc container in Azure DevOps
				if (changeList.size() > 0 && errCount == 0) {
					// Process work item changes in Azure DevOps
					log.info("${getCurTimestamp()} - Processing work item changes...")
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
					
					// Create the SmartDoc
					log.info("${getCurTimestamp()} - Creating SmartDoc: ${module.getTitle()}")
					def result = smartDocManagementService.createSmartDoc(module, tfsUrl, collection, tfsCollectionGUID, tfsProject, tfsProjectURI, tfsTeamGUID, tfsAltUser, tfsAltPassword, mrTemplate, mrFolder)
					if (result == null) {
						log.info("SmartDoc creation returned null")
					}
					else if (result.error != null && result.error.code != "null") {
						log.info("SmartDoc creation failed.  Error code: ${result.error.code}, Error message: ${result.error.message}, Error name: ${result.error.name}")
					}
					else {
						log.info("SmartDoc creation succeeded. Result: ${result.result}")
					}

				}
				
	
			}
			log.info("Processing completed")
		}


	}
	
	boolean isToIncorporateTitle(def module, def indexOfElementToCheck) {
		// This function is dependent upon the type of module
		String artifactType = module.orderedArtifacts[indexOfElementToCheck].getArtifactType()
		String moduleType = module.getArtifactType()
		boolean shouldMerge = false 
		if ((moduleType == 'Functional Spec') &&
			   (artifactType == 'Supporting Material' ||
				artifactType == 'Scope' ||
				artifactType == 'Out of Scope' ||
				artifactType == 'Assumption' ||
				artifactType == 'Issue' )) {
			shouldMerge = true 
		}
		else if ((moduleType == 'UI Spec') &&
			   (artifactType == 'Supporting Material' ||
				artifactType == 'Screen Change' ||
				artifactType == 'User Interface Flow'))	{
			shouldMerge = true 
		}
		return shouldMerge
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
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'tfs.altuser','tfs.altpassword', 'rm.mapping.file', 'rm.query', 'rm.filter', 'mr.url', 'mr.template', 'mr.folder']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
