package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RunXLBlueprint implements IExecutableYamlHandler {
	
	
	@Value('${xl.user:}')
	String xlUser
	
	@Value('${xl.password:}')
	String xlPassword

	public RunXLBlueprint() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations) {
		def answers = yaml.answers
		String xlDeployFile = "${repo.absolutePath}/${yaml.file}"
		List<String> values = []
		if (yaml.values) {
			yaml.values.each { val ->
				String valOut = "${val.name}=${val.'value'}"
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
		new AntBuilder().exec(dir: "${repo.absolutePath}", executable: "${command}", failonerror: true) {
			env( key:"https_proxy", value:"https://${xlUser}:${xlPassword}@172.18.4.115:8080")
			if (values.size() > 0) {
				arg( line: "${option} ${repo.absolutePath}/xl apply -p ${xlOutPath} -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}  --values ${valuesStr}")
			} else {
				arg( line: "${option} ${repo.absolutePath}/xl apply -p ${xlOutPath} -f ${xlDeployFile} --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}")
				
			}
		}
	}
	
	boolean performExecute(def yaml, List<String> locations) {		
		if (yaml.dependencies) return false
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
