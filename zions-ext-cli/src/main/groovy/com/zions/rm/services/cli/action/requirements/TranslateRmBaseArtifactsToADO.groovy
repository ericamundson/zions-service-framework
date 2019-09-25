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
import com.zions.rm.services.requirements.RequirementQueryData

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
 * @Autowired ClmRequirementsItemManagementService clmRequirementsItemManagementService
 * @Autowired ClmRequirementsManagementService clmRequirementsManagementService
 * @Autowired RequirementsMappingManagementService requirementsMappingManagementService
 * 
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
		//		String oslcNs = data.getOptionValues('oslc.namespaces')[0]
		//		String oslcSelect = data.getOptionValues('oslc.select')[0]
		//		String oslcWhere = data.getOptionValues('oslc.where')[0]
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
		if (includes['flushQueries'] != null) {
			log.info("Refreshing cache of main DNG query from JRS")
			clmRequirementsManagementService.flushQueries()
			log.info("Finished refreshing cache of main DNG query from JRS, future operations should use this cache")
		}
		if (includes['flushQueriesDelta'] != null) {
			log.info("Refreshing cache of main DNG query from JRS")
			clmRequirementsManagementService.flushQueries(true)
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
			int totalProcessedItems = 0
			int currentCount = 0
			int changeCount = 0 //kinda wanna put this in ChangeListManager but w/e
			ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
			restartManagementService.processPhases { phase, items ->
				//log.debug("Entering phase loop")
				clmRequirementsItemManagementService.resetNewId()
				
				if (phase == 'requirements') {
					log.debug("Getting content of ${items.size()} items")
					items.each { rmItem ->
						saveDatawarehouseItemToAdoItemManager(rmItem, clManager, tfsProject, memberMap)
					}
					log.debug("have ${clManager.size()} changes, about to flush clmanager for phaseCount ${phaseCount}")
					changeCount+= clManager.size()
					clManager.flush();
					log.debug("finished flushing clmanager for phaseCount ${phaseCount}")
				}
				if (phase == 'links') {
					items.each { rmItem ->
						//saveDatawarehouseItemToAdoItemManager(rmItem, clManager, tfsProject, memberMap)
						String sid = "${rmItem.reference_id}"
						
						clmRequirementsManagementService.getLinkInfoFromCache(sid)
						
						//get linkinfo object from cache
						//if linkinfo item count > 0
						//	do the needful for creating change objects with the links
						//sometimes this is blank?  some kind of error!
					}
				}
				if (phase == 'audit') {
					//log.debug("auditing migrated artifacts")
					//get list of wiData objects
					//MongoDBCacheManagementService.getAllOfType
					//def wiDataObjects = cacheManagementService.getAllOfType("wiData")
					currentCount = items.size()
					log.debug("Processing ${currentCount} items for page ${phaseCount}")
					items.each { rmItem ->
						String sid = "${rmItem.reference_id}"
						//sometimes this is blank?  some kind of error!
						if (sid) {
							//cacheManagementService.exists does not validate type, so we got whereUsed data in addition to wiData
							//we only want wiData here obv
							if (!cacheManagementService.getFromCache(sid, "wiData")) {
							//leaving this log line in because I can count the lines in the output file to see how we're doing
							log.debug("Datawarehouse item missing from mongodb: ${sid}, page:${phaseCount}, ${rmItem.about}")
							saveDatawarehouseItemToAdoItemManager(rmItem, clManager, tfsProject, memberMap)
							}
						} else {
							log.debug("Unable to find reference id for requirement? This error should not occur if phase loop data is corect")
						}
					}
					if (clManager.size() > 0) {
						changeCount += clManager.size()
						log.info("Reattempting upload of ${clManager.size()} artifacts from page ${phaseCount}")
						clManager.flush()
					}
					totalProcessedItems += currentCount
					log.info("Audited ${totalProcessedItems} DW artifacts so far, found ${changeCount} total in error and attempted reupload")
				}
				phaseCount++
			}
			printCheckpointErrorLogs()
		}
		
		if (includes['printSummary']) {	
			//this is too memory intensive, just need the actual count from mongo
			//workaround/current usage model is using the db explorer/cli
			//I am sure there is a way to do this here but I do not know it
			//def wiobjects = cacheManagementService.getAllOfType("wiData")
			//log.info("\r\n\r\nAfter the above corrections, ${wiobjects.size()} wiData objects were found in the cache\r\nThis should match the wi count in ADO\r\n")
			
			printCheckpointErrorLogs()
		}
	}
	
	/**
	 * Gets all checkpoint logs in checkpointManagementService and prints them
	 * @return
	 */
	def printCheckpointErrorLogs() {
		log.info("Attempting to find all error logs in current checkpoints...")
		try {
		def errorLogs = checkpointManagementService.getAllLogs()
		if (errorLogs) {
		log.info("The following items failed to upload and generated a checkpoint log:")
		
		errorLogs.each { logEntry ->
			log.info(logEntry)
		}
		} else {
			log.info("No errors were logged in the checkpoints!")
		}
		} catch (Exception e) {
			//I'm not sure that it will fail but we're about to prod release and I just don't want to bother with it if it does
			log.error("printCheckpointErrorLogs failed: ${e}")
		}
	}
	
	/**
	 * Processes 
	 * @param rmItem
	 * @param clManager
	 * @param tfsProject
	 * @param memberMap
	 * @return
	 */
	def saveDatawarehouseItemToAdoItemManager(def rmItem, def clManager, def tfsProject, def memberMap) {

			int id = Integer.parseInt(sid)
			//log.debug("items.each loop for id: ${sid}")
			String primaryTextString = "${rmItem.text}"
			//data warehouse indicator for wrapperresources is replacing the primay text field with this string
			String format = primaryTextString.equals("No primary text") ? 'WrapperResource' : 'Text'
			//here is the uri
			String about = "${rmItem.about}"
			//log.debug("Fetch artifact: ${sid} ${format} ${about}")
			ClmArtifact artifact = new ClmArtifact('', format, about)
			if (format == 'Text') {
				try {
					clmRequirementsManagementService.getTextArtifact(artifact,false,true)
				} catch (Exception e) {
					checkpointManagementService.addLogentry("${sid} : getTextArtifact for  generated an exception and was not added as a change")
					return
				}
			}
			else if (format == 'WrapperResource'){
				try {
					clmRequirementsManagementService.getNonTextArtifact(artifact,false,true)
				} catch (Exception e) {
					checkpointManagementService.addLogentry("${sid} : getNonTextArtifact generated an exception and was not added as a change")
					return
				}
			}
			else {
				checkpointManagementService.addLogentry("${sid} : Unsupported format of $format for artifact")
				return
			}

			//new FlowInterceptor() {}.flowLogging(clManager) {
			def reqChanges
			try {
				reqChanges = clmRequirementsItemManagementService.getChanges(tfsProject, artifact, memberMap)
			} catch (Exception e) {
				checkpointManagementService.addLogentry("${sid} : could not getChanges for because: ${e}")
				return
			}
			if (reqChanges) {
				reqChanges.each { key, val ->
					clManager.add("${id}", val)
				}
				//log.debug("adding changes for requirement ${id}")
			}
			//}
		} else {
			checkpointManagementService.addLogentry("Had an error getting the ID of an item, skipping.")
		}
	}


	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'clm.pageSize', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'rm.mapping.file', 'rm.filter', 'mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
}
