package com.zions.clm.services.cli.action.work

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder

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
 * <img src="TranslateRTCWorkToVSTSWork.png"/>
 * <p><b>Flow:</b></p>
 * <img src="TranslateRTCWorkToVSTSWork_sequence_diagram.png"/>
 * 
 * @author z091182
 *
 * @startuml TranslateRTCWorkToVSTSWork.png
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
 * @startuml TranslateRTCWorkToVSTSWork_sequence_diagram.png
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
class TranslateRTCWorkToVSTSWork implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
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

	public TranslateRTCWorkToVSTSWork() {
	}

	/**
	 * Executes integration action.
	 * 
	 * 
	 * <p><b>Flow:</b></p>
	 * <img src="TranslateRTCWorkToVSTSWork_execute_flow.png"/>
	 *  
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 * 
	 * @startuml TranslateRTCWorkToVSTSWork_execute_flow.png
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
		String tfsProject = data.getOptionValues('tfs.project')[0]
		File mFile = new File(mappingFile)

		def mapping = new XmlSlurper().parseText(mFile.text)
		def ccmWits = loadCCMWITs(templateDir)
		//Update TFS wit definitions.
		if (includes['meta'] != null) {
			def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, ccmWits)
		}
		//refresh.
		if (includes['refresh'] != null) {
			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def filtered = filtered(workItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					changeList.add(id)
				}
				def wiChanges = workManagementService.refreshCache(collection, tfsProject, changeList)
				def rel = workItems.@rel
				if ("${rel}" != 'next') break
					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
			}
		}
		//translate work data.
		if (includes['workdata'] != null) {
			def translateMapping = processTemplateService.getTranslateMapping(collection, tfsProject, mapping, ccmWits)
			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			while (true) {
				ccmWorkManagementService.resetNewId()
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(workItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					def wiChanges = ccmWorkManagementService.getWIChanges(id, tfsProject, translateMapping, memberMap)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = workItems.@rel
				if ("${rel}" != 'next') break
					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
			}
		}
		//		workManagementService.testBatchWICreate(collection, tfsProject)
		//apply work links
		if (includes['worklinks'] != null) {
			def linkMapping = processTemplateService.getLinkMapping(mapping)
			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(workItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					def wiChanges = ccmWorkManagementService.getWILinkChanges(id, tfsProject, linkMapping)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = workItems.@rel
				if ("${rel}" != 'next') break
					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
			}
		}

		//extract & apply attachments.
		if (includes['attachments'] != null) {
			def linkMapping = processTemplateService.getLinkMapping(mapping)
			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(workItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					def files = attachmentsManagementService.cacheWorkItemAttachments(id)
					def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = workItems.@rel
				if ("${rel}" != 'next') break
					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
			}
		}

		ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}

	def filtered(def workItems, String filter) {
		if (this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(workItems)
		}
		return workItems.workItem.findAll { wi ->
			true
		}
	}

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
