package com.zions.rm.services.cli.action.requirements

import java.util.Map

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import com.zions.clm.services.rtc.project.workitems.QueryTracking
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.rm.services.requirements.ClmRequirementsItemManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import com.zions.rm.services.requirements.ClmArtifact
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
 *  TranslateRmBaseArtifactsToADO -> WorkManagementService: send list of changes
 * end
 * @enduml
 */
@Component
@Slf4j
class TranslateRmBaseArtifactsToADO implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired 
	ClmRequirementsItemManagementService clmRequirementsItemManagementService
	@Autowired 
	ClmRequirementsManagementService clmRequirementsManagementService
	@Autowired 
	RequirementsMappingManagementService rmMappingManagementService
	@Autowired
	ClmRequirementsFileManagementService rmFileManagementService
	@Autowired
	IRestartManagementService restartManagementService
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	
	public TranslateRmBaseArtifactsToADO() {
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
		String mappingFile = data.getOptionValues('rm.mapping.file')[0]
		String oslcNs = data.getOptionValues('oslc.namespaces')[0]
		String oslcSelect = data.getOptionValues('oslc.select')[0]
		String oslcWhere = data.getOptionValues('oslc.where')[0]
		String rmFilter = data.getOptionValues('rm.filter')[0]
		String tfsProjectURI = data.getOptionValues('tfs.projectUri')[0]
		String tfsTeamGUID = data.getOptionValues('tfs.teamGuid')[0]
		String tfsCollectionGUID = data.getOptionValues('tfs.collectionId')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		File mFile = new File(mappingFile)
		
		def mapping = new XmlSlurper().parseText(mFile.text)
		if (includes['clean'] != null) {
		}
		//legacy stuff.
		if (includes['deprecated'] != null) {
			// Get field mappings, target members map and RM artifacts to translate to ADO
			log.info('Getting Mapping Data...')
			def mappingData = rmMappingManagementService.mappingData
			log.info('Getting ADO Project Members...')
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			log.info("Querying for Where Used Lookup ...")
			if (clmRequirementsManagementService.queryForWhereUsed()) {
				log.info("'where used' records were retrieved")
			}
			else {
				log.error('***Error retrieving "Where Used" lookup.  Check the log for details')
			}
			log.info("Querying DNG Base Artifacts ...")
			def results = clmRequirementsManagementService.queryForArtifacts(projectURI, oslcNs, oslcSelect, oslcWhere)
			// Continue until all pages have been processed
			def page = 1
			while (true) {
				def changeList = []
				def uploadedArtifacts = []
				def idMap = [:]
				int count = 0		
				results.Description.children().each { item ->
					if (item.Requirement != '') {
						def artifact = getItemChanges(tfsProject, item, memberMap)
						def aid = artifact.getCacheID()
						artifact.changes.each { key, val ->
							String idkey = "${aid}"
							idMap[count] = idkey
							changeList.add(val)
							count++		
						}
						
						// If uploaded artifact, save for attachment processing
						if (artifact.getFormat() == 'WrapperResource') {
							uploadedArtifacts.add(artifact)
						}
					}
				}
				log.info("$count Base Artifacts were retrieved")
				
				// Create work items in Azure DevOps
				if (changeList.size() > 0) {
					// Process work item changes in Azure DevOps
					log.info("Processing work item changes...")
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				
				// Upload Attachments to Azure DevOps
				log.info("Uploading attachments...")
				changeList.clear()
				idMap.clear()
				count = 0
				uploadedArtifacts.each { artifact ->
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
				log.info("$count Attachments were uploaded")
				if (changeList.size() > 0) {
					// Associate attachments to work items in Azure DevOps
					log.info("Associating attachments to work items...")
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				
				// Process next page
				String nextUrl = "${results.ResponseInfo.nextPage.@'rdf:resource'}"
				if (nextUrl != '') {
					page++
					log.info("Retrieving page ${page}...")
					results = clmRequirementsManagementService.nextPage(nextUrl)
				}
				else {
					break
				}
			}
				
			log.info("Processing completed")
		}
		// Refresh cached list of CLM artifacts to migrate to ADO
		if (includes['refresh'] != null) {
			log.info("Refreshing cache.")
			Checkpoint cp = cacheManagementService.getFromCache('query', 'QueryStart')
			int page=0
			Date currentTimestamp = new Date()
			if (cp) {
				currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.getTimeStamp())
				
			}
			String pageId = "${page}"
			def artifacts
			new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, QueryTracking) {
				artifacts = clmRequirementsManagementService.queryForArtifacts(projectURI, oslcNs, oslcSelect, oslcWhere)
			}
			while (true) {
				def changeList = []
//				def filtered = filtered(workItems, wiFilter)
				artifacts.Description.children().each { itemData ->
					int id = Integer.parseInt("${itemData.Requirement.identifier}")
					changeList.add(id)
				}
				def wiChanges = workManagementService.refreshCache(collection, tfsProject, changeList)
				String nextUrl = "${artifacts.ResponseInfo.nextPage.@'rdf:resource'}"
				if (nextUrl == '') break
				page++
				pageId = "${page}"
				new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, QueryTracking) {
					artifacts = clmRequirementsManagementService.nextPage(nextUrl)
				}
			}
		}
		if (includes['phases'] != null) {
			
			restartManagementService.processPhases { phase, items ->
				if (phase == 'query_whereused') {
					if (clmRequirementsManagementService.queryForWhereUsed()) {
						log.info("'where used' records were retrieved")
					}
					else {
						log.error('***Error retrieving "Where Used" lookup.  Check the log for details')
					}
				}

				if (phase == 'process_artifacts') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					clmRequirementsItemManagementService.resetNewId()
					items.each { rmItem ->
						String sid = "${rmItem.Requirement.identifier}"
						int id = Integer.parseInt(sid)
						String formatString = rmItem.Requirement.ArtifactFormat.@'rdf:resource'
						String format = formatString.substring(formatString.lastIndexOf('#') + 1)
						String about = "${rmItem.Requirement.@'rdf:about'}"
						ClmArtifact artifact = new ClmArtifact('', format, about)
						if (format == 'Text') {
							clmRequirementsManagementService.getTextArtifact(artifact,false)
						}
						else if (format == 'WrapperResource'){
							clmRequirementsManagementService.getNonTextArtifact(artifact)
						}
						else {
							log.info("WARNING: Unsupported format of $format for artifact id: $identifier")
						}
						//new FlowInterceptor() {}.flowLogging(clManager) {
							def wiChanges = clmRequirementsItemManagementService.getChanges(project, artifact, memberMap)
							if (wiChanges != null) {
								clManager.add("${id}", wiChanges)
							}
						//}
					}
					clManager.flush();
				}
			}
		}

			

	}

	def getItemChanges(String project, def rmItemData, def memberMap) {

		String modified = rmItemData.Requirement.modified
		String identifier = rmItemData.Requirement.identifier
		String formatString = rmItemData.Requirement.ArtifactFormat.@'rdf:resource'
		String format = formatString.substring(formatString.lastIndexOf('#') + 1)
		String about = "${rmItemData.Requirement.@'rdf:about'}"
		ClmArtifact artifact = new ClmArtifact('', format, about)
		if (format == 'Text') {
			clmRequirementsManagementService.getTextArtifact(artifact,false)
		}
		else if (format == 'WrapperResource'){
			clmRequirementsManagementService.getNonTextArtifact(artifact)
		}
		else {
			log.info("WARNING: Unsupported format of $format for artifact id: $identifier")
		}
		artifact.setChanges(clmRequirementsItemManagementService.getChanges(project, artifact, memberMap))
		return artifact
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
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'clm.pageSize', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'rm.mapping.file', 'oslc.namespaces', 'oslc.select', 'oslc.where', 'rm.filter', 'mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
