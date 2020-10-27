package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

/**
 * Provides command-line interations to extract work item meta-data from RTC.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>extractCcmWIMetadata - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - </li>
 *  <li>ccm.projectArea - RTC project area</li>
 *  <li>template.dir - Output directory for work item meta-data xml files.</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="ExtractCcmWIMetadata.svg"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class ExtractCcmWIMetadata [[java:com.zions.clm.services.cli.action.wit.ExtractCcmWIMetadata]] {
 * 	~CcmWIMetadataManagementService ccmWIMetadataManagementService
 * 	+ExtractCcmWIMetadata()
 * 	+def execute(ApplicationArguments data)
 * 	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * CliAction <|.. ExtractCcmWIMetadata
 * ExtractCcmWIMetadata --> com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService: @Autowired ccmWIMetadataManagementService
 * @enduml
 *
 */
@Component
class ExtractCcmWIMetadata implements CliAction {
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	
	public ExtractCcmWIMetadata() {
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 */
	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('clm.projectArea')[0]
		String templateDir = data.getOptionValues('template.dir')[0]
		def metadata = ccmWIMetadataManagementService.extractWorkitemMetadata(project, templateDir)
		ccmWIMetadataManagementService.rtcRepositoryClient.shutdownPlatform();
		return null;
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#validate(org.springframework.boot.ApplicationArguments)
	 */
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'template.dir' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
