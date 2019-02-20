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
class TranslateRRMToADO implements CliAction {
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
	
	public TranslateRRMToADO() {
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
		String mrTfsUrl = data.getOptionValues('mr.tfsUrl')[0]
		String projectURI = data.getOptionValues('clm.projectAreaUri')[0]
		String tfsUser = data.getOptionValues('tfs.user')[0]
		String mappingFile = data.getOptionValues('rm.mapping.file')[0]
		String rmQuery = data.getOptionValues('rm.query')[0]
		String rmFilter = data.getOptionValues('rm.filter')[0]
		String tfsProjectURI = data.getOptionValues('tfs.projectUri')[0]
		String tfsTeamGUID = data.getOptionValues('tfs.teamGuid')[0]
		String tfsCollectionGUID = data.getOptionValues('tfs.collectionId')[0]
		String tfsOAuthToken = data.getOptionValues('tfs.oAuthToken')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		String mrTemplate = data.getOptionValues('mr.template')[0]
		String mrFolder = data.getOptionValues('mr.folder')[0]
		File mFile = new File(mappingFile)
		
		def mapping = new XmlSlurper().parseText(mFile.text)
		if (includes['clean'] != null) {
		}
		//refresh.
		if (includes['refresh'] != null) {
		}
		//translate work data.
		if (includes['data'] != null) {
			// Get field mappings, target members map and RM modules to translate to ADO
			println('Getting Mapping Data...')
			def mappingData = rmMappingManagementService.mappingData
			println('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			println("${getCurTimestamp()} - Querying DNG Modules for $rmQuery ...")
			def modules = clmRequirementsManagementService.queryForModules(projectURI, rmQuery)
			def changeList = []
			def idMap = [:]
			int count = 0
			modules.each { module ->
				println("${getCurTimestamp()} - Processing Module: ${module.getTitle()} (${count + 1} of ${modules.size()}) ...")
				// Check all artifacts for "Heading"/"Row type" inconsistencies, then abort on this module if any were found
				def errCount = 0
				module.orderedArtifacts.each { artifact ->
					if (artifact.getIsHeading() && artifact.getArtifactType() != 'Heading' ) {
						println("*** ERROR: Artifact #${artifact.getID()} has inconsistent row type in module ${module.getTitle()}")
						errCount++
					}
				}
				if (errCount > 0) {
					println("*** ERROR: Skipping module '${module.getTitle()}' due to $errCount errors")
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
				int it = 0 // we have to use our own "it" since Groovy won't allow an implicit "it" to be incremented
				while(errCount == 0) {

					// If Heading is immediately followed by Supporting Material, move Heading title to Supporting Material and logically delete Heading artifact
					if (module.orderedArtifacts[it].getIsHeading() && 
						it < module.orderedArtifacts.size()-1 && 
						isToIncorporateTitle(module,it+1)) {
						
						module.orderedArtifacts[it+1].setTitle(module.orderedArtifacts[it].getTitle())
						module.orderedArtifacts[it].setIsDeleted(true)
						it++  // Skip Heading artifact 
					}
					else if (module.orderedArtifacts[it].getIsHeading()) {
						module.orderedArtifacts[it].setDescription("") // If simple heading, remove duplicate description
					}
					else if (it > 0 && module.orderedArtifacts[it].getDepth() <= module.orderedArtifacts[it-1].getDepth()){ 
						// For all other content, increment the depth if not already incremented so as to preserve outline numbering from DOORS
						module.orderedArtifacts[it].incrementDepth(1)
					}
					if (!module.checkForDuplicate(it)) {  // Only store first occurrence of an artifact in the module
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
						println("Skipping duplicate requirement #${module.orderedArtifacts[it].getID()}")
					}
					if (it >= module.orderedArtifacts.size() - 1) {
						break
					}
					it++
				}
				


				// Create work items and SmartDoc container in Azure DevOps
				if (changeList.size() > 0 && errCount == 0) {
					// Process work item changes in Azure DevOps
					println("${getCurTimestamp()} - Processing work item changes...")
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
					
					// Create the SmartDoc
					println("${getCurTimestamp()} - Creating SmartDoc: ${module.getTitle()}")
					def result = smartDocManagementService.createSmartDoc(module, collection, mrTfsUrl, tfsCollectionGUID, tfsProject, tfsProjectURI, tfsTeamGUID, tfsOAuthToken, mrTemplate, mrFolder)
					if (result == null) {
						println("SmartDoc creation returned null")
					}
					else if (result.error != null && result.error.code != "null") {
						println("SmartDoc creation failed.  Error code: ${result.error.code}, Error message: ${result.error.message}, Error name: ${result.error.name}")
					}
					else {
						println("SmartDoc creation succeeded. Result: ${result.result}")
					}
					
					
					// Upload Attachments to Azure DevOps
					println("${getCurTimestamp()} - Uploading attachments...")
					changeList.clear()
					idMap.clear()
					count = 0
					module.orderedArtifacts.each { artifact ->
						if (artifact.getFormat() == 'WrapperResource' && !artifact.getIsDuplicate()) {
							def files = []
							files[0] = rmFileManagementService.cacheRequirementFile(artifact)
							
							String id = artifact.getCacheID()
							def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
							if (wiChanges != null) {
								def url = "${wiChanges.body[1].value.url}"
								def change = [op: 'add', path: '/fields/System.Description', value: '<div><a href=' + url + '&download=true>Uploaded Attachment</a></div>']
								wiChanges.body.add(change)
								idMap[count] = "${id}"
								changeList.add(wiChanges)
								count++
						
							}
							
						}
					}
					if (changeList.size() > 0) {
						// Associate attachments to work items in Azure DevOps
						println("${getCurTimestamp()} - Associating attachments to work items...")
						workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
					}
				}
				

			}
			println("Processing completed")

		}

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
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
				artifactType == 'Screen Change' ))	{
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
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'tfs.oAuthToken', 'rm.mapping.file', 'rm.query', 'rm.filter', 'mr.url', 'mr.tfsUrl', 'mr.template', 'mr.folder']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
