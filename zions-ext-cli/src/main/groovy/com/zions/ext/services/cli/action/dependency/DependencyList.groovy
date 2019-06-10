package com.zions.ext.services.cli.action.dependency

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

/**
 * Quick command-line utility to build a gradle dependency list from a folder of jars.
 * 
 * This was used for module zions-ccm-client-service gradle.build file.
 * 
 * @author z091182
 *
 */
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
