package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.core.env.Environment
import groovy.util.logging.Slf4j

@Component
@Slf4j
class RunXLDeployApply implements IExecutableYamlHandler {
	
	@Autowired
	Environment env
	
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
		Boolean useProxy = yaml.useProxy
		if (!useProxy) {
			useProxy = false
		}
		List<String> values = []
		if (yaml.values) {
			yaml.values.each { val ->
				String value = "${val.value}"
				if (value.startsWith('${')) {
					String name = value.substring('${'.length())
					name = name.substring(0, name.length() - 1)
					value = env.getProperty(name)
				}
				String valOut = "${val.name}=${value}"
				values.add(valOut)
			}
		}
		String valuesStr = values.join(',')
		
		loadXLCli(repo)
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
			 dir: "${repo.absolutePath}", 
			 executable: "${command}", 
			 failonerror: false) {
			if (useProxy) {
				env( key:"https_proxy", value:"http://${xlUser}:${xlPassword}@172.18.4.115:8080")
			}
			if (values.size() > 0) {
				arg( line: "${option} ${repo.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}")
			} else {
				arg( line: "${option} ${repo.absolutePath}/xl apply  -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}")
				
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
