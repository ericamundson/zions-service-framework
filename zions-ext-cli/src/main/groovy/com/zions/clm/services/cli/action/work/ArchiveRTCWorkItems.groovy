package com.zions.clm.services.cli.action.work

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.QueryTracking
import com.zions.common.services.excel.ExcelManagementService
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.excel.ExcelManagementService
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.test.TestDataInterceptor
import com.zions.rm.services.requirements.ClmArtifact
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

/**
 * Provides command line interaction to archive RTC work items to Excel.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>archiveRTCWorkItems - The action's Spring bean name.</li>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - (optional) CLM password. It can be hidden in props file.</li>
 *  <li>ccm.projectArea - RQM project area</li>
 *  <li>ccm.template.dir - RTC meta-data xml</li>
 *  <li>wit.mapping.file - The xml mapping file to enable field data flow.</li>
 *  <li>wi.query - The xpath RQM testplan query.</li>
 *  <li>wi.filter - the name of filter class to used to pair down items that can't be filtered by query.</li>
 *  <li>include.update - Comma delimited list of the phases that will run during execution. E.G. meta,refresh,clean,workdata,worklinks,attachments</li>
 *  </ul>
 * </ul>
 * 
 */
@Component
@Slf4j
class ArchiveRTCWorkItems implements CliAction {
//	@Autowired
//	private Map<String, IFilter> filterMap;
	@Autowired
	ProcessTemplateService processTemplateService;
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	@Autowired
	CcmWorkManagementService ccmWorkManagementService
	@Autowired
	AttachmentsManagementService attachmentsManagementService

	@Autowired
	IRestartManagementService restartManagementService
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	@Autowired
	ExcelManagementService excelManagementService
	@Autowired
	@Value('${excel.path}')
	String filePath

	public ArchiveRTCWorkItems() {
	}

	/**
	 * Executes integration action.
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
		File mFile = new File(mappingFile)

		def mapping = new XmlSlurper().parseText(mFile.text)
		def ccmWits = loadCCMWITs(templateDir)
		if (includes['flushQueries'] != null) {
			clmWorkItemManagementService.flushQueries(wiQuery)
		}
//		boolean testData = false
//		def translateMapping
//		new TestDataInterceptor() {}.provideTestData(processTemplateService, './src/test/resources/testdata', !testData, ['getTranslateMapping', 'getLinkMapping']) {
//			translateMapping = processTemplateService.getTranslateMapping(mapping, ccmWits)
//		}

		//Archive work data.
		if (includes['phases'] != null) {
			String lastArtifactType
			restartManagementService.processPhases { phase, items ->
				if (phase == 'workdata' || phase == 'update') {
					ccmWorkManagementService.resetNewId()
					items.each { workitem ->
						int id = Integer.parseInt(workitem.id)
						String sid = "${workitem.id}"
						IWorkItem ccmWorkitem = ccmWorkManagementService.getWorkitem(sid)
						Date ts = ccmWorkitem.modified()
						//	log.debug("LastArtifactType: $lastArtifactType and this artifact: $curArtifactType")
						if (!lastArtifactType.equals(ccmWorkitem.getWorkItemType())) {
							String curArtifactType = "${ccmWorkitem.getWorkItemType()}"
							log.info("New artifact type: ${curArtifactType}")
							//create new document
							excelManagementService.CreateExcelFile(filePath,curArtifactType)
							lastArtifactType = curArtifactType
						}
						def wiAttributes = ccmWorkManagementService.getWIAttributes(ccmWorkitem,project)
						insertArtifactIntoExcel(sid, lastArtifactType, wiAttributes)
					}
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
			excelManagementService.CloseExcelFile()
//			printCheckpointErrorLogs()
		}
		ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}
	

//	def filtered(def workItems, String filter) {
//		if (this.filterMap[filter] != null) {
//			return this.filterMap[filter].filter(workItems)
//		}
//		return workItems.workItem.findAll { wi -> true }
//	}

	def insertArtifactIntoExcel(String id, String type, def attributeMap) {
		//each attribute must be inserted into the workbook without making it too messy
		//perhaps ExcelManager can add columns to current row based on insertion by header
		//log.debug("wi made it to insert method")
		//log.debug("'ID': ${wi.ID}, 'wi Type': ${wi.wiType}")
		excelManagementService.InsertNewRow(['Id': "$id", 'Type': "$type"])
		attributeMap.each { key, value ->
			try {
			excelManagementService.InsertIntoCurrentRow(value, key)
			} catch (Exception e){
				log.error("Artifact $id had error writing to spreadsheet: ${e}")
			}
		}
		//create new row
		//foreach attribute in artifact {
		//  insert into current row: (attribute, attributeType as header)
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
		def required = ['clm.url', 'clm.user', 'clm.projectArea', 'ccm.template.dir', 'wit.mapping.file', 'wi.query', 'wi.filter']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
