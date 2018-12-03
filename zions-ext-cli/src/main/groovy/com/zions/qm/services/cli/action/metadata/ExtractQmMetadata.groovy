package com.zions.qm.services.cli.action.metadata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.qm.services.metadata.QmMetadataManagementService

import groovy.json.JsonBuilder

/**
 * Extract RQM REST API meta-data.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>extractQmMetadata - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - (optional) CLM password. It can be hidden in props file.</li>
 *  <li>qm.projectArea - Rational Quality Manager project area</li>
 *  <li>qm.template.dir - File system directory to place extracted meta-data.</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="ExtractQmMetadata.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class ExtractQmMetadata [[java:com.zions.qm.services.cli.action.metadata.ExtractQmMetadata]] {
 * 	~QmMetadataManagementService qmMetadataManagementService
 * 	+ExtractQmMetadata()
 * 	+def execute(ApplicationArguments data)
 * 	+Object validate(ApplicationArguments args)
 * }
 * interface CliAction [[java:com.zions.common.services.cli.action.CliAction]] {
 * }
 * CliAction <|.. ExtractQmMetadata
 * @enduml
 *
 */
@Component
class ExtractQmMetadata implements CliAction {
	@Autowired
	QmMetadataManagementService qmMetadataManagementService;
	
	public ExtractQmMetadata() {
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 */
	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('qm.projectArea')[0]
		String templateDir = data.getOptionValues('qm.template.dir')[0]
		File tDir = new File(templateDir)
		if (tDir.exists()) {
			def metadata = qmMetadataManagementService.extractQmMetadata(project, tDir)
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#validate(org.springframework.boot.ApplicationArguments)
	 */
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'qm.projectArea', 'qm.template.dir' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
