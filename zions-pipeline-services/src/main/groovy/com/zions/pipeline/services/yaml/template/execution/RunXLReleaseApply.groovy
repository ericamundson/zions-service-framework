package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

import com.zions.common.services.vault.VaultService
import com.zions.pipeline.services.mixins.CliRunnerTrait
import com.zions.pipeline.services.mixins.XLCliTrait

import groovy.util.logging.Slf4j

@Component
@Slf4j
/**
 * Yaml should be in this form:
 * 
 * type: runXLReleaseApply
 * yamlFile: .pipeline/xl-deploy.yaml
 * vault:  # Optional yaml object to setup token replacement from Vault
 *   engine: secret
 *   path: WebCMS # a path into Vault secret store that can be project specific.  
 * values: # Optional for setting up XL CLI values.
 * - name: test1
 *   value: ${xl.password}  # xl.password token to be replaced by Vault
 * - name: test2
 *   value: avalue2
 *   
 *   This implementation will also search for secrets.xlvals file and perform token replace of secrets 
 *   from Vault values.
 *   
 * @author z091182
 *
 */class RunXLReleaseApply implements IExecutableYamlHandler, CliRunnerTrait, XLCliTrait {
	@Autowired
	Environment env
	
	@Autowired
	VaultService vaultService

	@Value('${xlr.url:https://xlrelease.cs.zionsbank.com}')
	String xlrUrl
	
	@Value('${xl.user:}')
	String xlUser
	
	@Value('${xl.password:}')
	String xlPassword
	
	@Value('${xlr.use.proxy:false}')
	boolean xlrUseProxy

	public RunXLReleaseApply() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations, String branch, String project) {
		if (!performExecute(yaml, locations)) return
		
		boolean useProxy = xlrUseProxy
		if (yaml.useProxy) {
			useProxy = yaml.useProxy
		}
		def vaultSecrets = null
		if (yaml.vault) {
			vaultSecrets = vaultService.getSecrets(yaml.vault.engine, yaml.vault.path)
		} else {
			vaultSecrets = vaultService.getSecrets('secret', project)
		}
		//String xlOutPath = "${yaml.path}"
		String xlReleaseFile = "${repo.absolutePath}/${yaml.yamlFile}"
		String wdir = xlReleaseFile.substring(0, xlReleaseFile.lastIndexOf('/'))
		List<String> values = []
		if (yaml.values) {
			yaml.values.each { val ->
				String value = "${val.value}"
				if (value.startsWith('${') && vaultSecrets) {
					String name = value.substring('${'.length())
					name = name.substring(0, name.length() - 1)
					def oval = vaultSecrets[name]
					if (oval) {
						value = oval
					}
				}
				String valOut = "${val.name}=${value}"
				values.add(valOut)
			}
		}
		String valuesStr = values.join(',')
		processSecrets(wdir, vaultSecrets)
		
		File wdirF = new File(wdir)
		loadXLCli(wdirF)
		String os = System.getProperty('os.name')
		String command = 'cmd'
		String option = '/c'
		if (!os.contains('Windows')) {
			command = '/bin/sh'
			option = '-c'
		}
		def arg = null
		if (values.size() > 0) {
			arg = [line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlReleaseFile} --xl-release-url ${xlrUrl} --xl-release-username ${xlUser} --xl-release-password ${xlPassword}  --values ${valuesStr}"]
		} else {
			arg = [ line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlReleaseFile} --xl-release-url ${xlrUrl} --xl-release-username ${xlUser} --xl-release-password ${xlPassword}" ]
			
		}
		def env = null
		if (useProxy) {
			env = [key:"https_proxy", value:"http://${xlUser}:${xlPassword}@172.18.4.115:8080"]
		}
		run(command, "${wdirF.absolutePath}", arg, env, log)
		
	}
	def processSecrets(String wdir, vaultSecrets) {
		File wdirF = new File(wdir)
		File secretsFile = new File(wdirF, '/xebialabs/secrets.xlvals')
		if (secretsFile.exists() && vaultSecrets) {
			Properties props = new Properties()
			def is = secretsFile.newDataInputStream()
			props.load(is)
			is.close()
			for (String name in props.stringPropertyNames()) {
				def val = props.getProperty(name)
				String value = "${val}"
				if (value.startsWith('${')) {
					String aname = value.substring('${'.length())
					aname = aname.substring(0, aname.length() - 1)
					value = vaultSecrets[aname]
					if (value) {
						props.setProperty(name, value)
					}
				}
			}
			def w = secretsFile.newWriter()
			props.store(w, 'updated secrets')
			w.close()
		}
	}

	boolean performExecute(def yaml, List<String> locations) {		
		if (!yaml.dependencies || locations.size() == 0) return true
		for (String dep in yaml.dependencies) {
			if (locations.contains(dep)) return true
		}
		return false
	}
	
}
