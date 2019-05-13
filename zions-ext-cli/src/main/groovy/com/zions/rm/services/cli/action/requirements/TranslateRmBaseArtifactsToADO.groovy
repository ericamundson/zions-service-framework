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
import com.zions.common.services.db.DatabaseQueryService
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
import com.zions.rm.services.requirements.RequirementQueryData
import com.zions.common.services.db.DataWarehouseQueryData

/**
 * Provides command line interaction to synchronize RRM requirements management with ADO.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>translateRRMToADO - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The followimodular ng's command-line format: --name=value</b></p>
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
	WorkManagementService workManagementService
	@Autowired 
	ClmRequirementsItemManagementService clmRequirementsItemManagementService
	@Autowired 
	ClmRequirementsManagementService clmRequirementsManagementService
	@Autowired
	DatabaseQueryService rmDatabaseQueryService
	@Autowired
	ClmRequirementsFileManagementService rmFileManagementService
	@Autowired
	IRestartManagementService restartManagementService
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	
	String dbQueryString = '''SELECT T1.REFERENCE_ID as reference_id,
  T1.URL as about,
  T1.Primary_Text as text
FROM RIDW.VW_REQUIREMENT T1
LEFT OUTER JOIN RICALM.VW_RQRMENT_ENUMERATION T2
ON T2.REQUIREMENT_ID=T1.REQUIREMENT_ID AND T2.NAME='Release'
WHERE T1.PROJECT_ID = 19  AND
(  T1.REQUIREMENT_TYPE NOT IN ( 'Change Request','Actor','Use Case','User Story','Spec Proxy','Function Point','Process Inventory','Term','Use Case Diagram' ) AND
  T2.LITERAL_NAME = 'Deposits'  AND
  LENGTH(T1.URL) = 65 AND
  T1.REC_DATETIME > TO_DATE('05/01/2014','mm/dd/yyyy')
) AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_ID <> -1 AND T1.REQUIREMENT_ID IS NOT NULL)
'''
	
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
		
		log.info('Getting ADO Project Members...')
		def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)

		if (includes['clean'] != null) {
			String query = "Select [System.Id], [System.Title] From WorkItems Where [Custom.ExternalID] CONTAINS 'DNG-'"
			if (areaPath.length()>0) {
				query = query + " AND [System.AreaPath] = '${areaPath}'"
			}
			workManagementService.clean(collection,tfsProject, query)
		}

		// Refresh cached list migrated ADO work items
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
			new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, RequirementQueryData) {
				artifacts = clmRequirementsManagementService.queryForArtifacts(projectURI, oslcNs, oslcSelect, oslcWhere)
			}
			while (true) {
				def changeList = []
//				def filtered = filtered(workItems, wiFilter)
				artifacts.Description.children().each { itemData ->
					String sid = "${itemData.Requirement.identifier}"
					if (sid != '' && sid != null) {
						int id
						try {
							id = Integer.parseInt("${itemData.Requirement.identifier}")
						}
						catch (NumberFormatException e) { 
							log.error("NumberFormatException trying to parse identifier: ${itemData.Requirement.identifier}")
							return
						}
						changeList.add(id)
					}
				}
				log.info("Refreshing cache for ${changeList.size()} artifacts.")
				def wiChanges = workManagementService.refreshCache(collection, tfsProject, changeList)
				//I am not sure how relevant the below set of work is
				//that would be part of flushQueries, no?
				String nextUrl = "${artifacts.ResponseInfo.nextPage.@'rdf:resource'}"
				if (nextUrl == '') break
				page++
				pageId = "${page}"
				//stores entire result page in cache I believe
				new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, QueryTracking) {
					artifacts = clmRequirementsManagementService.nextPage(nextUrl)
				}
			}
		}
		if (includes['flushQueries'] != null) {
			log.info("Refreshing cache of main DNG query from JRS")
			rmDatabaseQueryService.flushQueries()
			log.info("Finished refreshing cache of main DNG query from JRS, future operations should use this cache")
		}
		if (includes['whereused'] != null) {
			log.info("fetching 'where used' lookup records")
			if (clmRequirementsManagementService.queryForWhereUsed()) {
				log.info("'where used' records were retrieved")
			}
			else {
				log.error('***Error retrieving "Where Used" lookup.  Check the log for details')
			}
		}
		if (includes['phases'] != null) {
			
			log.info("Processing artifacts")
			int phaseCount = 0
			restartManagementService.processPhases { phase, items ->
				log.debug("Entering phase loop")
				if (phase == 'requirements') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					clmRequirementsItemManagementService.resetNewId()
					//rmDatabaseQueryService.init()
					log.debug("Getting content of ${items.size()} items")
					items.each { rmItem ->
						String sid = "${rmItem.reference_id}"
						//sometimes this is blank?  some kind of error!
						if (sid) {
							int id = Integer.parseInt(sid)
							//log.debug("items.each loop for id: ${sid}")
							String primaryTextString = "${rmItem.text}"
							//data warehouse indicator for wrapperresources is replacing the primay text field with this string
							String format = primaryTextString.equals("No primary text") ? 'WrapperResource' : 'Text'
							//here is the uri
							String about = "${rmItem.about}"
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
								def reqChanges = clmRequirementsItemManagementService.getChanges(tfsProject, artifact, memberMap)
								if (reqChanges != null) {
									reqChanges.each { key, val ->
										clManager.add("${id}", val)
										
									}
									//log.debug("adding changes for requirement ${id}")
								}
							//}
						} else {
							log.debug("Had an error getting the ID of an item, skipping")
							//todo: add an error to the checkpoint error log
						}
					}
					log.debug("have ${clManager.size()} changes, about to flush clmanager for phaseCount ${phaseCount}")
					clManager.flush();
					log.debug("finished flushing clmanager for phaseCount ${phaseCount}")
					phaseCount++
				}
			}
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
