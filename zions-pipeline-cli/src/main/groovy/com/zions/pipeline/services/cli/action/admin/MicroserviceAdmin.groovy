/**
 * 
 */
package com.zions.pipeline.services.cli.action.admin

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.pipeline.services.mixins.CliRunnerTrait

import java.util.regex.Pattern
import java.util.regex.Matcher


import groovy.util.logging.Slf4j

/**
 * @author z091182
 *
 */
@Component
@Slf4j
class MicroserviceAdmin implements CliAction, CliRunnerTrait {
	@Value('${services.dir:}')
	File servicesDir
	
	@Value('${admin.action:restart}')
	String adminAction
	
	@Value('${admin.exclude.list:}')
	String[] adminExcludeList

	public def execute(ApplicationArguments data) {
		List eList = Arrays.asList(adminExcludeList)
		def filePattern = Pattern.compile('^\\S.*(.exe)$')
		
		if (servicesDir.exists()) {
			log.info("has services dir")
			
		} else {
			log.info("has services dir doesn't exist")
		}
		servicesDir.eachDir() { File aserviceDir ->
			aserviceDir.eachFileMatch(filePattern) { File exe ->
				MicroserviceAdmin.log.info("Found exe:  ${exe.name}")
				String eName = "${exe.name}"
				if (!eList.contains(eName)) {
					String command = 'cmd'
					String option = '/c'
					def args = [line: "${option} ${exe.name} ${adminAction}"]
					try {
						run(command, "${aserviceDir.absolutePath}", args, null, MicroserviceAdmin.log)
					} catch (e) {
						e.printStackTrace()
					}
				}
			}
		}
	}
	
	
	
	public Object validate(ApplicationArguments args) throws Exception {
	}
}
