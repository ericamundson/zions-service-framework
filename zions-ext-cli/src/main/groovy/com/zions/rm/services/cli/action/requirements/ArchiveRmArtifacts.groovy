package com.zions.rm.services.cli.action.requirements

import java.util.Map

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import com.zions.clm.services.rtc.project.workitems.QueryTracking
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.excel.ExcelManagementService
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

@Component
@Slf4j
class ArchiveRmArtifacts implements CliAction {
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
	@Autowired
	ExcelManagementService excelManagementService
	@Autowired
	@Value('${excel.path}')
	String filePath
	
	public ArchiveRmArtifacts() {
	}

	public def execute(ApplicationArguments data) {
		def includes = [:]
		try {
			String includeList = data.getOptionValues('include.update')[0]
			def includeItems = includeList.split(',')
			includeItems.each { item ->
				includes[item] = item
			}
		} catch (e) {}
		String projectURI = data.getOptionValues('clm.projectAreaUri')[0]
		String rmFilter = data.getOptionValues('rm.filter')[0]
		
		if (includes['flushQueries'] != null) {
			log.info("Refreshing cache of main DNG query from JRS")
			clmRequirementsManagementService.flushQueries()
			log.info("Finished refreshing cache of main DNG query from JRS, future operations should use this cache")
		}
		if (includes['phases'] != null) {
			log.info("Processing artifacts")
			int phaseCount = 0
			String lastArtifactType = ''
			restartManagementService.processPhases { phase, items ->	
				if (phase == 'archive') {
//					log.debug("Getting content of ${items.size()} items")
					items.each { rmItem ->
						//get item from DNG
						ClmArtifact artifact = getArtifactFromSource(rmItem)
						//make sure it came back correctly
						if (artifact) {
							if (!lastArtifactType.equals(artifact.getArtifactType())) {
								lastArtifactType = artifact.getArtifactType()
								//close old excel document - just put this in the open call I think
								//excelManagementService.CloseExcelFile()
								//create new excel document
								excelManagementService.CreateExcelFile(filePath,lastArtifactType)
							}
							insertArtifactIntoExcel(artifact)
						} else {
							//I doubt this will fire
							log.warn("DNG Artifact ${rmItem.reference_id} was not retrieved properly")
						}
						
				//pseudocode:
//						if artifact.type != lastArtifactType
//							- close old excel document
//						  - create new excel document 
//						download artifact details from clm

						// this might be a file
						//insert into current excel document
						//save current artifact type as lastArtifactType
					}
				}
				phaseCount++
			}
			excelManagementService.CloseExcelFile()
		}
	}
	
	ClmArtifact getArtifactFromSource(def jrsItem) {
		String sid = "${jrsItem.reference_id}"
		ClmArtifact artifact
		if (sid) {
			int id = Integer.parseInt(sid)
			String primaryTextString = "${jrsItem.text}"
			String format = primaryTextString.equals("No primary text") ? 'WrapperResource' : 'Text'
			String about = "${jrsItem.about}"
			artifact = new ClmArtifact('', format, about)
			if (format == 'Text') {
				try {
					clmRequirementsManagementService.getTextArtifact(artifact,false,true)
				} catch (Exception e) {
					checkpointManagementService.addLogentry("${sid} : getTextArtifact for  generated an exception and was not added as a change")
				}
			}
			else if (format == 'WrapperResource'){
				try {
					clmRequirementsManagementService.getNonTextArtifact(artifact,false,true)
				} catch (Exception e) {
					checkpointManagementService.addLogentry("${sid} : getNonTextArtifact generated an exception and was not added as a change")
				}
			}
			else {
				checkpointManagementService.addLogentry("${sid} : Unsupported format of $format for artifact")
			}
			return artifact
		}
	}
	
	def insertArtifactIntoExcel(ClmArtifact artifact) {
		//each attribute must be inserted into the workbook without making it too messy
		//perhaps ExcelManager can add columns to current row based on insertion by header
		log.debug("artifact made it to insert method")
		excelManagementService.InsertNewRow(['ID': "${artifact.ID}", 'Artifact Type': "${artifact.ArtifactType}"])
		//create new row
		//foreach attribute in artifact {
		//  insert into current row: (attribute, attributeType as header)
		
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'clm.pageSize']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
}
