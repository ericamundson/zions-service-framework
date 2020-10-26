package com.zions.clm.services.cli.action.work

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.QueryTracking
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.test.TestDataInterceptor
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

/**
 * Provides command line interaction to synchronize RTC work items with ADO.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>translateRTCWorkToVSTSWork - The action's Spring bean name.</li>
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
 *  <li>ccm.template.dir - RTC meta-data xml</li>
 *  <li>tfs.areapath - ADO area path to set Test planning items.</li>
 *  <li>wit.mapping.file - The xml mapping file to enable field data flow.</li>
 *  <li>wi.query - The xpath RQM testplan query.</li>
 *  <li>wi.filter - the name of filter class to used to pair down items that can't be filtered by query.</li>
 *  <li>include.update - Comma delimited list of the phases that will run during execution. E.G. meta,refresh,clean,workdata,worklinks,attachments</li>
 *  </ul>
 * </ul>
 * 
 * 
 * <p><b>Design:</b></p>
 * <img src="TranslateRTCWorkToVSTSWork.svg"/>
 * <p><b>Flow:</b></p>
 * <img src="TranslateRTCWorkToVSTSWork_sequence_diagram.svg"/>
 * 
 * @author z091182
 *
 * @startuml TranslateRTCWorkToVSTSWork.svg
 * annotation Autowired
 * annotation Component
 * class TranslateRTCWorkToVSTSWork [[java:com.zions.clm.services.cli.action.work.TranslateRTCWorkToVSTSWork]] {
 *	+TranslateRTCWorkToVSTSWork()
 *	+def execute(ApplicationArguments data)
 *	~def filtered(def workItems, String filter)
 *	~def loadCCMWITs(def ccmTemplateDir)
 *	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * class Map<String, IFilter> {
 * }
 * TranslateRTCWorkToVSTSWork ..> Autowired
 * TranslateRTCWorkToVSTSWork ..> Component
 * CliAction <|.. TranslateRTCWorkToVSTSWork
 * TranslateRTCWorkToVSTSWork --> Map: @Autowired filterMap a test
 * TranslateRTCWorkToVSTSWork --> com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService: @Autowired ccmWIMetadataManagementService
 * TranslateRTCWorkToVSTSWork --> com.zions.vsts.services.work.templates.ProcessTemplateService: @Autowired processTemplateService
 * package com.zions.vsts.services.work {
 * 	TranslateRTCWorkToVSTSWork --> WorkManagementService: @Autowired workManagementService
 *  TranslateRTCWorkToVSTSWork --> FileManagementService: @Autowired fileManagementService
 * }
 * TranslateRTCWorkToVSTSWork --> com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService: @Autowired clmWorkItemManagementService
 * TranslateRTCWorkToVSTSWork --> com.zions.clm.services.ccm.workitem.CcmWorkManagementService: @Autowired ccmWorkManagementService
 * TranslateRTCWorkToVSTSWork --> com.zions.vsts.services.admin.member.MemberManagementService: @Autowired memberManagementService
 * TranslateRTCWorkToVSTSWork --> com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService: @Autowired attachmentsManagementService
 * @enduml
 * 
 * @startuml TranslateRTCWorkToVSTSWork_sequence_diagram.svg
 * 
 * participant CliApplication
 * CliApplication -> TranslateRTCWorkToVSTSWork: validate(ApplicationArguments args)
 * CliApplication -> TranslateRTCWorkToVSTSWork: execute(ApplicationArguments args)
 * alt include.update has 'clean'
 * 	TranslateRTCWorkToVSTSWork -> WorkManagementService: clean up added work items
 * end
 *  alt include.update has 'workdata'
 *  TranslateRTCWorkToVSTSWork -> ProcessTemplateService: get field mapping
 *  TranslateRTCWorkToVSTSWork -> MemberManagementService: get member map
 *  TranslateRTCWorkToVSTSWork -> ClmWorkitemManagementService: get work items via query
 *  loop each { work item  }
 *  	TranslateRTCWorkToVSTSWork -> CcmWorkManagementService: get data changes
 *  	TranslateRTCWorkToVSTSWork -> List: add changes to list
 *  end
 *  TranslateRTCWorkToVSTSWork -> WorkManagementService: send list of changes to wi batch.
 *  end
 *  alt include.update has 'worklinks'
 *  TranslateRTCWorkToVSTSWork -> ProcessTemplateService: get field mapping
 *  TranslateRTCWorkToVSTSWork -> MemberManagementService: get member map
 *  TranslateRTCWorkToVSTSWork -> ClmWorkitemManagementService: get work items via query
 *  loop each { work item  }
 *  	TranslateRTCWorkToVSTSWork -> CcmWorkManagementService: get link data changes
 *  	TranslateRTCWorkToVSTSWork -> List: add changes to list
 *  end
 *  TranslateRTCWorkToVSTSWork -> WorkManagementService: send list of changes to wi batch.
 *  end
 *  alt include.update has 'attachments'
 *  TranslateRTCWorkToVSTSWork -> ProcessTemplateService: get field mapping
 *  TranslateRTCWorkToVSTSWork -> MemberManagementService: get member map
 *  TranslateRTCWorkToVSTSWork -> ClmWorkitemManagementService: get work items via query
 *  loop each { work item  }
 *  	TranslateRTCWorkToVSTSWork -> AttachmentsManagementService: save CLM attachment to cache
 *  	TranslateRTCWorkToVSTSWork -> FileManagementService: ensure attachment associated to ADO and provide wi changes
 *  	TranslateRTCWorkToVSTSWork -> List: Add attachment change to batch list.
 *  end
 *  TranslateRTCWorkToVSTSWork -> WorkManagementService: send list of changes to wi batch.
 *  end
 *  @enduml
 */
@Component
@Slf4j
class TranslateRTCWorkToVSTSWork implements CliAction {
//	@Autowired
//	private Map<String, IFilter> filterMap;
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	@Autowired
	ProcessTemplateService processTemplateService;
	@Autowired
	WorkManagementService workManagementService;
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	@Autowired
	CcmWorkManagementService ccmWorkManagementService
	@Autowired
	MemberManagementService memberManagementService
	@Autowired
	AttachmentsManagementService attachmentsManagementService
	@Autowired
	FileManagementService fileManagementService
	@Autowired(required=false)
	TestManagementService testManagementService
//	@Autowired(required=false)
//	CacheInterceptorService cacheInterceptorService

	@Autowired
	IRestartManagementService restartManagementService
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	@Autowired(required=false)
	ICacheManagementService cacheManagementService

	public TranslateRTCWorkToVSTSWork() {
	}

	/**
	 * Executes integration action.
	 * 
	 * 
	 * <p><b>Flow:</b></p>
	 * <img src="TranslateRTCWorkToVSTSWork_execute_flow.svg"/>
	 *  
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 * 
	 * @startuml TranslateRTCWorkToVSTSWork_execute_flow.svg
	 * participant CLI 
	 * CLI -> TranslateRTCWorkToVSTSWork: execute(applicationArguments)
	 * @enduml
	 */
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
		String project = data.getOptionValues('clm.projectArea')[0]
		String templateDir = data.getOptionValues('ccm.template.dir')[0]
		String mappingFile = data.getOptionValues('wit.mapping.file')[0]
		String wiQuery = data.getOptionValues('wi.query')[0]
		String wiFilter = data.getOptionValues('wi.filter')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String cleanQuery = null
		try {
			cleanQuery = data.getOptionValues('clean.query')[0]
		} catch (e) {}
		String areaPath = ""
		try {
			areaPath = data.getOptionValues('tfs.areapath')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		File mFile = new File(mappingFile)

		def mapping = new XmlSlurper().parseText(mFile.text)
		def ccmWits = loadCCMWITs(templateDir)
//		//Update TFS wit definitions.
//		if (includes['meta'] != null) {
//			def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, ccmWits)
//		}
		//refresh.
		if (includes['refresh'] != null) {
			log.info("Refreshing cache.")
			workManagementService.refreshCacheByTeamArea(collection, tfsProject, areaPath)
		}
		if (includes['flushQueries'] != null) {
			clmWorkItemManagementService.flushQueries(wiQuery)
		}
		if (includes['clean'] != null) {
			String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${tfsProject}' AND [Custom.ExternalID] CONTAINS 'RTC-'"
			if (areaPath.length()>0) {
				query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${tfsProject}' AND [System.AreaPath] = '${areaPath}' AND [Custom.ExternalID] CONTAINS 'RTC-'"
			}
			if (cleanQuery) {
				query = cleanQuery
			}
			workManagementService.clean(collection, tfsProject, query)
		}
//		if (includes['cleanDuplicates'] != null) {
//			workManagementService.cleanDuplicates(collection, tfsProject)
//		}
		boolean testData = false
		def translateMapping
		new TestDataInterceptor() {}.provideTestData(processTemplateService, './src/test/resources/testdata', !testData, ['getTranslateMapping', 'getLinkMapping']) {
			translateMapping = processTemplateService.getTranslateMapping(collection, tfsProject, mapping, ccmWits)
		}
		def memberMap		
		new TestDataInterceptor() {}.provideTestData(memberManagementService, './src/test/resources/testdata', !testData, ['getProjectMembersMap']) {
			memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
		}
		def linkMapping = processTemplateService.getLinkMapping(mapping)
		//translate work data.
		if (includes['phases'] != null) {
			//cacheManagementService.deleteByType('LinkInfo')
			restartManagementService.processPhases { phase, items ->
				if (phase == 'workdata' || phase == 'update') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					ccmWorkManagementService.resetNewId()
					items.each { workitem ->
						int id = Integer.parseInt(workitem.id)
						String sid = "${workitem.id}"
						def ccmWorkitem = ccmWorkManagementService.getWorkitem(sid)
						Date ts = ccmWorkitem.modified()
						def links = ccmWorkManagementService.getAllLinks(sid, ts, ccmWorkitem, linkMapping)
						//new FlowInterceptor() {}.flowLogging(clManager) {
						def wiChanges = ccmWorkManagementService.getWIChanges(id, tfsProject, translateMapping, memberMap)
						def files = attachmentsManagementService.cacheWorkItemAttachments(id)
						wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files, wiChanges)
						if (wiChanges != null) {
							clManager.add("${id}", wiChanges)
						}
					}
					clManager.flush();
				}
				//apply work links
				if (phase == 'worklinks' || phase == 'update' || phase == 'other') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def linkItems = [:]
					items.each { workitem ->
						int id = Integer.parseInt(workitem.id)
						ccmWorkManagementService.getWILinkChanges(id, tfsProject, linkMapping) { key, changes ->
							if (key == 'WorkItem') {
								//cleanRelated(linkItems, changes)
								clManager.add("${id}", changes)
							} else if (key == 'Result') {
								def resultChanges = changes.resultChanges
								def rid = changes.rid
								cacheManagementService.cacheModule = 'QM'
								testManagementService.sendResultChanges(collection, tfsProject, resultChanges, rid)
								cacheManagementService.cacheModule = 'CCM'
								workManagementService.refreshCache(collection, tfsProject, ["${id}"])
							}
						}
					}
					clManager.flush();
				}
				

				//extract & apply attachments.
//				if (phase == 'attachments' || phase == 'update' || phase == 'other') {
//					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
//					items.each { workitem ->
//						int id = Integer.parseInt(workitem.id)
//						def files = attachmentsManagementService.cacheWorkItemAttachments(id)
//						def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
//						if (wiChanges != null) {
//							clManager.add("${id}",wiChanges)
//						}
//					}
//					clManager.flush()
//				}
			}
		}
		ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}
	
	def cleanRelated(linkItems, changes) {
		String inId = "${changes.uri}"
		inId = inId.substring(inId.lastIndexOf('/')+1)
		inId = inId.substring(0, inId.lastIndexOf('?'))
		linkItems[inId] = changes
		log.info( "WI id being added for check: ${inId}")
		changes.body.each { link ->
			if (link.value && link.path == '/relations/-') {
				String id = "${link.value.url}"
				id = id.substring(id.lastIndexOf('/')+1)
				String json = new JsonBuilder(link).toPrettyString()
				log.info("WI id link being checked: ${id}\n ${json}")
				if (linkItems[id]) {
					def markToDelete = linkItems[id]
					def dItem = markToDelete.body.find { mlink ->
						boolean flag = false
						if (mlink.value && mlink.path == '/relations/-') {
							String mid = "${mlink.value.url}"
							mid = mid.substring(mid.lastIndexOf('/')+1)
							if (mid == inId) {
								flag = true
							}
						}
						flag
					}
					if (dItem) {
						markToDelete.body.remove(dItem)
					}
				}
			}
		}
	}


//	def filtered(def workItems, String filter) {
//		if (this.filterMap[filter] != null) {
//			return this.filterMap[filter].filter(workItems)
//		}
//		return workItems.workItem.findAll { wi -> true }
//	}

	def loadCCMWITs(def ccmTemplateDir) {
		def wits = []
		File tDir = new File(ccmTemplateDir)
		if (tDir.exists() || tDir.isDirectory()) {
			tDir.eachFile { file ->
				def witData = new XmlSlurper().parse(file)
				wits.add(witData)
			}
		}
		return wits
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.projectArea', 'ccm.template.dir', 'tfs.url', 'tfs.user', 'tfs.project', 'wit.mapping.file', 'wi.query', 'wi.filter']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
