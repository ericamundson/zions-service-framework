package com.zions.pipeline.services.mixins

import org.springframework.beans.factory.annotation.Value

import groovy.yaml.YamlBuilder

trait XLCliTrait extends CliRunnerTrait {
	
	@Value('${blueprint.config.context:Test}')
	String configContext
		
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
			
			String command = '/usr/bin/sudo'
			String option = '-u scmuser'
			def args = [ line: "${option} chmod 777 xl" ]
			run(command, "${loadDir.absolutePath}", args)
		}
	}
	
	def buildConfigYaml(File loadDir, File bpRepo) {
		File configYaml = new File(loadDir, '.xebialabs/config.yaml')
		File cDir = new File(loadDir, '.xebialabs')
		if (!cDir.exists()) {
			cDir.mkdirs()
		}
		
		def config = new YamlBuilder()
		config {
			blueprint {
				currentrepository "${configContext}"
				repositories(["${configContext}"]) { repo ->
					if (configContext == 'Dev') {
						name 'Dev'
						type 'local'
						path "${bpRepo.absolutePath}"
						ignoredirs '.git,.vscode'
						ignorefiles '.DS_Store,.gitignore'
					} else if (configContext == 'Test') {
						name 'Test'
						type 'http'
						url "http://localhost:8090/repository/zions-blueprints"
					} else if (configContext == "Prod") {
						name 'Prod'
						type 'http'
						url "https://nexus.cs.zionsbank.com/repository/zions-blueprints"
					}
				}
			}
		}
		
		String cStr = config.toString()
		cStr = cStr.substring('---\n'.length())
		cStr = cStr.replace('currentrepository', 'current-repository')
		cStr = cStr.replace('ignoredirs', 'ignore-dirs')
		cStr = cStr.replace('ignorefiles', 'ignore-files')
		//File aF = new File(outDir, "${pipelineFolder}/answers.yaml")
		println cStr
		println configYaml.absolutePath
		def sAF = configYaml.newDataOutputStream()
		sAF << cStr
		sAF.close()

	}
	
	def genIndex(File bpRepo) {
		File index = new File(bpRepo, 'index.json')
		def jyF = index.newDataOutputStream();
		jyF << "[\n"
		bpRepo.eachDirRecurse() { File dir ->
			//if (dir.name == '.git') return
			String indexStr = '[\n'
			dir.eachFile { File file ->
				if (file.name == 'blueprint.yaml') {
					File parentFile = file.parentFile
					String parentName = parentFile.name
					String parentPath = parentFile.absolutePath
					String opath = parentPath.substring(bpRepo.absolutePath.length()+1)
					//println "opath: ${opath}, parentName: ${parentName}"
					jyF << "\"${opath}\",\n"
				}
			}
		}
		jyF << "]\n"
		jyF.close()
	}
	
	def updateIgnore(File repo) {
		File ignore = new File(repo, '.gitignore')
		Set ignoreItems = []
		if (ignore.exists()) {
			ignore.eachLine { line ->
				ignoreItems.add(line)
			}
			
		}
		ignore.withWriter { BufferedWriter writer ->
			if (!ignoreItems.contains('xl.exe')) {
				writer.writeLine 'xl.exe'
			}
			if (!ignoreItems.contains('xl')) {
				writer.writeLine 'xl'
			}
			if (!ignoreItems.contains('.xebialabs')) {
				writer.writeLine '.xebialabs'
			}
			if (!ignoreItems.contains('startup.bat')) {
				writer.writeLine 'startup.bat'
			}
		}
		
	}
	
	def fixBlueprint(File bpRepo, String folderName) {
		String oss = System.getProperty('os.name')
		if (oss.contains('Windows')) {
			folderName = folderName.replace('/','\\')
			File bpFile = new File(bpRepo, "${folderName}\\blueprint.yaml")
			if (bpFile.exists()) {
				String bpText = bpFile.text
				def items = (bpText =~ /[-]\s+blueprint:\s+\S+/).findAll()
				
				for (String item in items) {
					String citem = item.replace('/', '\\')
					bpText = bpText.replace(item, citem)
				}

				def wos = bpFile.newDataOutputStream()
				wos << bpText
				wos.close()
			}
		}
	}
}
