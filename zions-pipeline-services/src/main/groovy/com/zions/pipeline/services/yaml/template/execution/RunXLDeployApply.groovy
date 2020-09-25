package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.core.env.Environment
import groovy.util.logging.Slf4j
import com.zions.common.services.vault.VaultService

/**
 * Yaml should be in this form:
 * 
 * type: runXLDeployApply
 * yamlFile: .pipeline/xl-deploy.yaml
 * vault:  # Optional yaml object to setup token replacement from vault
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
 */
@Component
@Slf4j
class RunXLDeployApply implements IExecutableYamlHandler {
	
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

	public RunXLDeployApply() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations, String branch, String project) {
		if (!performExecute(yaml, locations)) return
		//String xlOutPath = "${yaml.path}"
		String xlDeployFile = "${repo.absolutePath}/${yaml.yamlFile}"
		String wdir = xlDeployFile.substring(0, xlDeployFile.lastIndexOf('/'))
		Boolean useProxy = yaml.useProxy
		if (!useProxy) {
			useProxy = false
		}
		
		def vaultSecrets = null
		if (yaml.vault) {
			vaultSecrets = vaultService.getSecrets(yaml.vault.engine, yaml.vault.path)
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
		AntBuilder ant = new AntBuilder()
		ant.exec(outputproperty:"text",
             errorproperty: "error",
             resultproperty: "exitValue", 
			 dir: "${wdirF.absolutePath}", 
			 executable: "${command}", 
			 failonerror: false) {
			if (useProxy) {
				env( key:"https_proxy", value:"http://${xlUser}:${xlPassword}@172.18.4.115:8080")
			}
			if (values.size() > 0) {
				arg( line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}")
			} else {
				arg( line: "${option} ${wdirF.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}")
				
			}
		}
		def result = new Expando(
			text: ant.project.properties.text,
			error: ant.project.properties.error,
			exitValue: ant.project.properties.exitValue as Integer,
			toString: { text }
		)
		
		if (result.exitValue != 0) {
			throw new Exception("""command failed with ${result.exitValue}
error: ${result.error}
text: ${result.text}""")
		} else {
			log.info(result.text)
		}
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
		if (!yaml.dependencies) return true
		for (String dep in yaml.dependencies) {
			if (locations.contains(dep)) return true
		}
		return false
	}
	
	def loadXLCli(File loadDir) {
		String osname = System.getProperty('os.name')
			
		if (osname.contains('Windows')) {
			InputStream istream = this.getClass().getResourceAsStream('/xl/windows/xl.exe')
			File of = new File(loadDir, 'xl.exe')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()
		} else {
			InputStream istream = this.getClass().getResourceAsStream('/xl/linux/xl')
			File of = new File(loadDir, 'xl')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()

		}
	}
}
