package com.zions.common.services.command

import org.springframework.stereotype.Component

@Component
class CommandManagementService {
	public CommandManagementService() {
		
	}
	
	def executeCommand(String command, File dir) {
		def proc = "${command}".execute(null, dir)
		proc.waitForProcessOutput(System.out, System.err)

	}

}
