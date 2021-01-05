package com.zions.pipeline.services.mixins

import groovy.yaml.YamlBuilder

trait XLCliTrait extends CliRunnerTrait {
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
				currentrepository 'Zions'
				repositories(['Zions']) { repo ->
					name repo.toString()
					type 'local'
					path "${bpRepo.absolutePath}"
					ignoredirs '.git,.vscode'
					ignorefiles '.DS_Store,.gitignore'
				}
			}
		}
		
		String cStr = config.toString()
		cStr = cStr.substring('---\n'.length())
		cStr = cStr.replace('currentrepository', 'current-repository')
		cStr = cStr.replace('ignoredirs', 'ignore-dirs')
		cStr = cStr.replace('ignorefiles', 'ignore-files')
		//File aF = new File(outDir, "${pipelineFolder}/answers.yaml")
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
					jyF << "${opath},\n"
				}
			}
		}
		jyF << "]\n"
		jyF.close()
	}
}
