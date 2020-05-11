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
class ZeusQAProvision implements CliAction {
	@Value('${prod.version:}')
	String prodVersion
	
	@Value('${release.version:}')
	String releaseVersion
	
	@Value('${testbed.dir:}')
	String testbedDir
	
	List affiliates = ['AZ-NBA', 'CA-CBT', 'CO-VBC', 'NV-NSB', 'TX-ABT', 'UT-ZFNB']
	
	public def execute(ApplicationArguments data) {
		File prDir = new File("${testbedDir}/${prodVersion}PR")
		if (!prDir.exists()) {
			prDir.mkdirs()
		}
		for (String affiliate in affiliates) {
			new AntBuilder().copy( todir: "${testbedDir}/${prodVersion}PR/${affiliate}", preservelastmodified: true, overwrite: true ) {
				fileset( dir: "${testbedDir}/${prodVersion}/${affiliate}" ) {
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
		File rDir = new File("${testbedDir}/${releaseVersion}")
		if (!rDir.exists()) {
			rDir.mkdirs()
		}
		for (String affiliate in affiliates) {
			new AntBuilder().copy( todir: "${testbedDir}/${releaseVersion}/${affiliate}", preservelastmodified: true, overwrite: true ) {
				fileset( dir: "${testbedDir}/${prodVersion}PR/${affiliate}" ) {
//					include(name: "*/**")
//					exclude(name: "**/Zaddat/0*.*")
//					exclude(name: "**/Zaddat/1*.*")
//					exclude(name: "**/Zaddat/*.b\$?")
//					exclude(name: "**/Zaddat/Session.*")
//					exclude(name: "**/Zaddat/ZadVer.*")
//					exclude(name: "**/ZConfig/Brchcnfg.upg")
				}
			}
		}
		File rAutoDir = new File("${testbedDir}/${releaseVersion}Auto")
		if (!rAutoDir.exists()) {
			rAutoDir.mkdirs()
		}
		for (String affiliate in affiliates) {
			new AntBuilder().copy( todir: "${testbedDir}/${releaseVersion}Auto/${affiliate}", preservelastmodified: true, overwrite: true ) {
				fileset( dir: "${testbedDir}/${prodVersion}PR/${affiliate}" ) {
//					include(name: "*/**")
//					exclude(name: "**/Zaddat/0*.*")
//					exclude(name: "**/Zaddat/1*.*")
//					exclude(name: "**/Zaddat/*.b\$?")
//					exclude(name: "**/Zaddat/Session.*")
//					exclude(name: "**/Zaddat/ZadVer.*")
//					exclude(name: "**/ZConfig/Brchcnfg.upg")
				}
			}
		}

	}


	public Object validate(ApplicationArguments args) throws Exception {

	}
}
