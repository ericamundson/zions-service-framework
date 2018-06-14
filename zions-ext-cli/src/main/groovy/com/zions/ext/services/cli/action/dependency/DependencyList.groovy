package com.zions.ext.services.cli.action.dependency

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.clm.services.work.maintenance.service.FixWorkItemIssuesService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

@Component
class DependencyList implements CliAction {
	
	@Autowired
	public DependencyList() {
	}

	public def execute(ApplicationArguments data) {
		String depDir = data.getOptionValues('dep.dir')[0]
		String outFile = data.getOptionValues('out.file')[0]
		File df = new File(depDir)
		File of = new File(outFile)
		def o = of.newOutputStream();
		def nl = System.getProperty("line.separator")
		df.list().each { file -> 
			o << "compile files('libs/${file})')${nl}"
		}
		o.close()
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['dep.dir', 'out.file' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
