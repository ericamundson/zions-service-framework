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
class ZeusBLProvision implements CliAction {
	@Value('${prod.version:}')
	String prodVersion
	
	@Value('${release.version:}')
	String releaseVersion
	
	@Value('${bl.dir:}')
	String blDir
	
	
	public def execute(ApplicationArguments data) {
		buildOutTestBed(releaseVersion)
		buildOutTestBed(prodVersion)
	}
	
	def buildOutTestBed(String name) {
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh create_template.nsh -t ${name}")
		}
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh create_component_discovery_job.nsh ${name}")
		}
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh create_template_instances.nsh -t ${name}")
		}
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh discover_template_job.nsh -j ${name}")
		}
		new AntBuilder().exec(dir: "${blDir}", executable: 'cmd', failonerror: true) {
			arg( line: "/c nsh create_affiliate_components.nsh -t ${name}")
		}

	}


	public Object validate(ApplicationArguments args) throws Exception {

	}
}
