package com.zions.pipeline.services.cli.action.deploy


import groovy.util.logging.Slf4j


import com.zions.pipeline.services.mixins.CliRunnerTrait
import com.zions.pipeline.services.mixins.XLCliTrait
import org.springframework.stereotype.Component

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments

import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.vault.VaultService

@Component
@Slf4j
class XldApply implements CliAction, XLCliTrait, CliRunnerTrait {
	
	@Autowired
	VaultService vaultService
	
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
	
	@Value('${vault.paths:zions-service-framework}')
	String[] vaultPaths
	
	
	public def execute(ApplicationArguments data) {
		File xlFile = new File("${buildSourcesDirectory.absolutePath}/${xlFileName}")
		if (!xlFile.exists()) return
		File xlFileParent = new File(xlFile.parent)
		loadXLCli(xlFileParent)
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
		String[] sValues = convertSecrets()
		if (sValues.length > 0) {
			String valuesStr = sValues.join(',')
			arg = [line: "${option} ${xlFileParent.absolutePath}/xl apply  -f ${buildSourcesDirectory.absolutePath}/${xlFileName} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}"]
		} else {
			arg = [ line: "${option} ${xlFileParent.absolutePath}/xl apply -f ${buildSourcesDirectory.absolutePath}/${xlFileName} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}" ]
			
		}
		
		log.info( "CLI: ${arg.line}")
			
		run(command, "${xlFileParent.absolutePath}", arg, env, log)
	}
	
	String[] convertSecrets() {
		Map vaultSecrets = vaultService.getSecrets('secret', vaultPaths)
		def values = []
		for (String item in xlValues) {
			String[] keyVal = item.split('=')
			String key = keyVal[0].trim()
			String value = keyVal[1].trim()
			if (value.startsWith('${') && vaultSecrets) {
				String name = value.substring('${'.length())
				name = name.substring(0, name.length() - 1)
				value = vaultSecrets[name]
			}
			String valOut = "${key}=${value}"
			values.add(valOut)

		}
		return values as String[]
	}
	
	public def validate(ApplicationArguments args) throws Exception {
		
	}
}
