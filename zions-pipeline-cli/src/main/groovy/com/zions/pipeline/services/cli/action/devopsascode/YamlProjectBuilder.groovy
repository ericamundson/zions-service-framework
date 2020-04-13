package com.zions.pipeline.services.cli.action.devopsascode

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction

import groovy.util.logging.Slf4j

@Component
@Slf4j
class YamlProjectBuilder implements CliAction {
	
	@Value('${definition.yaml:}')
	File definitionYaml
	
	
	
	public def execute(ApplicationArguments data) {
		
	}
	
	public Object validate(ApplicationArguments args) throws Exception {
		
	}
}
