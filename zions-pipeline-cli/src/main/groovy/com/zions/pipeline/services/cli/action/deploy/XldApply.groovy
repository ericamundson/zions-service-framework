package com.zions.pipeline.services.cli.action.deploy


import groovy.util.logging.Slf4j


import com.zions.pipeline.services.mixins.CliRunnerTrait
import com.zions.pipeline.services.mixins.XLCliTrait
import org.springframework.stereotype.Component

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments

import com.zions.common.services.cli.action.CliAction

@Component
@Slf4j
class XldApply implements CliAction, XLCliTrait, CliRunnerTrait {
	
	@Value('${xl.file:.pipeline/xl-apps.yaml}')
	String xlFileName
	
	@Value('${build.sourcesdirectory:}')
	File buildSourcesDirectory
	
	@Value('${xld.url:https://xldeploy.cs.zionsbank.com}')
	String xldUrl
	
	@Value('${xl.user:}')
	String xlUser
	
	@Value('${xl.password:}')
	String xlPassword
	
	@Value('${xld.use.proxy:false}')
	boolean xldUseProxy
	
	@Value('${xl.values:}')
	String[] xlValues
	
	public def execute(ApplicationArguments data) {
		loadXLCli(buildSourcesDirectory)
		String os = System.getProperty('os.name')
		String command = 'cmd'
		String option = '/c'
		if (!os.contains('Windows')) {
			command = '/bin/sh'
			option = '-c'
		}
		def env = null
		if (xldUseProxy) {
			env = [key:"https_proxy", value:"https://${xlUser}:${xlPassword}@172.18.4.115:8080"]
		}
		def arg = [:]
		if (xlValues.size() > 0) {
			String valuesStr = xlValues.join(',')
			arg = [line: "${option} ${buildSourcesDirectory.absolutePath}/xl apply  -f ${buildSourcesDirectory.absolutePath}/${xlFileName} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}"]
		} else {
			arg = [ line: "${option} ${buildSourcesDirectory.absolutePath}/xl apply -f ${buildSourcesDirectory.absolutePath}/${xlFileName} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}" ]
			
		}
		run(command, "${buildSourcesDirectory.absolutePath}", arg, env, log)
	}
	
	public def validate(ApplicationArguments args) throws Exception {
		
	}
}
