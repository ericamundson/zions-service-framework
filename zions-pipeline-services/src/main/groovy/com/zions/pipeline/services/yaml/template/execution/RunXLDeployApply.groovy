package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.core.env.Environment
import groovy.util.logging.Slf4j
import com.zions.common.services.vault.VaultService
import com.zions.pipeline.services.mixins.CliRunnerTrait
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.pipeline.services.mixins.XLCliTrait

/**
 * Yaml should be in this form:
 * 
 * type: runXLDeployApply
 * yamlFile: .pipeline/xl-deploy.yaml
 * vault:  # Optional yaml object to setup token replacement from vault
 *   engine: secret
 *   paths: 
 *   - WebCMS   
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
 */
@Component
@Slf4j
class RunXLDeployApply implements IExecutableYamlHandler, CliRunnerTrait, XLCliTrait, FeedbackTrait {
	
	@Autowired
	Environment env
	
	@Autowired
	VaultService vaultService
	
	@Value('${xld.url:https://xldeploy.cs.zionsbank.com}')
	String xldUrl
	
	@Value('${xl.user:}')
	String xlUser
	
	@Value('${xl.password:}')
	String xlPassword
	
	@Value('${xld.use.proxy:false}')
	boolean xldUseProxy

	public RunXLDeployApply() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations, String branch, String project, String pipelineId = null) {
		if (!performExecute(yaml, locations)) return
		if (yaml.project) {
			project = yaml.project
		}
		
		//String xlOutPath = "${yaml.path}"
		String xlDeployFile = "${repo.absolutePath}/${yaml.yamlFile}"
		String wdir = xlDeployFile.substring(0, xlDeployFile.lastIndexOf('/'))
		Boolean useProxy = xldUseProxy
		if (yaml.useProxy) {
			useProxy = yaml.useProxy
		}
		
		def vaultSecrets = null
		if (yaml.vault) {
			vaultSecrets = vaultService.getSecrets(yaml.vault.engine, yaml.vault.paths as String[])
		} else {
			vaultSecrets = vaultService.getSecrets('secret', [project] as String[])
		}
		
		List<String> values = []
		if (yaml.values) {
			yaml.values.each { val ->
				String value = "${val.value}"
				if (value.startsWith('${') && vaultSecrets) {
					String name = value.substring('${'.length())
					name = name.substring(0, name.length() - 1)
					value = vaultSecrets[name]
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
		def arg = [:]
		if (values.size() > 0) {
			arg = [line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}"]
		} else {
			arg = [ line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}" ]
			
		}
		def env = null
		if (useProxy) {
			env = [key:"https_proxy", value:"http://${xlUser}:${xlPassword}@172.18.4.115:8080"]
		}
		run(command, "${wdirF.absolutePath}", arg, env, log, pipelineId)
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
