package com.zions.pipeline.services.cli.action.provision

import org.springframework.boot.ApplicationArguments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.common.services.cli.action.CliAction
import com.zions.xld.services.ci.CIService
import static groovy.io.FileType.*

@Component
@Slf4j
class ZeusUATProvision implements CliAction {
	
	@Value('${testbed.dir:}')
	String testbedDir
	
	
	public def execute(ApplicationArguments data) {
		
		String ts = new Date().format("yyyyMMdd_hhmmss")
		File pDir = new File("${testbedDir}/ZeusProd_${ts}")
		if (!pDir.exists()) {
			pDir.mkdirs()
		}
		new AntBuilder().move( todir: "${testbedDir}/ZeusProd_${ts}", preservelastmodified: true, overwrite: true ) {
			fileset( dir: "${testbedDir}/ZeusProd" ) {
			}
		}

		File prDir = new File("${testbedDir}/ZeusProd")
		if (prDir.exists()) {
			prDir.delete()
		}
	    prDir.mkdirs()
		
		new AntBuilder().copy( todir: "${testbedDir}/ZeusProd", preservelastmodified: true, overwrite: true ) {
			fileset( dir: "${testbedDir}/ZeusDev" ) {
				include(name: "*/**")
				exclude(name: "**/Zaddat/0*.*")
				exclude(name: "**/Zaddat/1*.*")
				exclude(name: "**/Zaddat/*.b\$?")
				exclude(name: "**/Zaddat/Session.*")
				exclude(name: "**/Zaddat/ZadVer.*")
				exclude(name: "**/ZConfig/Brchcnfg.upg")
			}
		}

	}


	public Object validate(ApplicationArguments args) throws Exception {

	}
}
