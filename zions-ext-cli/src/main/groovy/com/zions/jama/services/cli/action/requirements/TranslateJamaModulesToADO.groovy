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
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired
	JamaRequirementsManagementService jamaRequirementsManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	SmartDocManagementService smartDocManagementService
	@Autowired
	ClmRequirementsFileManagementService rmFileManagementService
	
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
		if (includes['phases'] != null) {
			def results = javaRequirementsManagementService.query()
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
		def required = ['clm.url', 'clm.user', 'clm.projectAreaUri', 'tfs.user', 'tfs.projectUri', 'tfs.teamGuid', 'tfs.url', 'tfs.collection', 'tfs.collectionId', 'tfs.user', 'tfs.project', 'tfs.areapath', 'tfs.serverAltCreds', 'rm.mapping.file', 'rm.query', 'rm.filter', 'mr.url', 'mr.template', 'mr.folder']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
