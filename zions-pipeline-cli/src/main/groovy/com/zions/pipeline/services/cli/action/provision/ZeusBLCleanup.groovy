package com.zions.pipeline.services.cli.action.provision

import org.springframework.boot.ApplicationArguments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.common.services.cli.action.CliAction
import com.zions.xld.services.ci.CiService
import static groovy.io.FileType.*

@Component
@Slf4j
class ZeusBLCleanup implements CliAction {
	
	@Value('${build.number:}')
	String buildNumber
	
	@Value('${bl.dir:}')
	String blDir
	
	
	public def execute(ApplicationArguments data) {
		runCleanup(buildNumber)
	}
	
	def runCleanup(String name) {
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh cleanup.nsh ${name}")
		}

	}


	public Object validate(ApplicationArguments args) throws Exception {

	}
}
