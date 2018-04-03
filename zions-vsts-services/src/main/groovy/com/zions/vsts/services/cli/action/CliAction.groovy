package com.zions.vsts.services.cli.action

import org.springframework.boot.ApplicationArguments

interface CliAction {
	def execute(ApplicationArguments args);
	def validate(ApplicationArguments args) throws Exception;
}
