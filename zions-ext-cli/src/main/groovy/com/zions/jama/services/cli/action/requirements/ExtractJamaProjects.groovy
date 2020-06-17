package com.zions.jama.services.cli.action.requirements

import java.util.Map
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.jama.services.requirements.JamaRequirementsManagementService
import com.zions.jama.services.requirements.JamaRequirementsFileManagementService
import com.zions.jama.services.requirements.JamaRequirementsItemManagementService
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
class ExtractJamaProjects implements CliAction {
//	@Autowired
//	private Map<String, IFilter> filterMap;
	@Autowired
	MemberManagementService memberManagementService;
	@Autowired
	JamaRequirementsManagementService jamaRequirementsManagementService;
	@Autowired
	JamaRequirementsItemManagementService jamaRequirementsItemManagementService;
	@Autowired
	JamaRequirementsFileManagementService jamaFileManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	SmartDocManagementService smartDocManagementService
	@Autowired
	IGenericRestClient jamaGenericRestClient

	public ExtractJamaProjects() {
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
		String tfsProjectURI = data.getOptionValues('tfs.projectUri')[0]
		String tfsProject = data.getOptionValues('tfs.project')[0]
		String tfsUrl = data.getOptionValues('tfs.url')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
				
		def projects = jamaRequirementsManagementService.getAllProjects()
		projects.each { project ->
			if (project.isFolder == false) {
				def parent
				def created = "${project.createdDate}".replace('.000+0000','')
				def modified = "${project.createdDate}".replace('.000+0000','')
				log.info(	"${project.id},$parent,$created,$modified,${jamaRequirementsManagementService.getUserEmail(project.createdBy)},${jamaRequirementsManagementService.getUserEmail(project.modifiedBy)},${project.fields.name},${project.fields.statusId}"		
						)
			}
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
		def required = ['tfs.user', 'tfs.projectUri', 'tfs.url', 'tfs.collection', 'mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
