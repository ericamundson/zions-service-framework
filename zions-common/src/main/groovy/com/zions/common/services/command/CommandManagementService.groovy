package com.zions.common.services.command

import org.springframework.stereotype.Component

/**
 * Class to handle making command line calls.  Also provide the ability to make classes using this
 * more testable.
 * 
 * @author z091182
 *
 */
@Component
class CommandManagementService {
	public CommandManagementService() {
		
	}
	
	def executeCommand(String command, File dir) {
		def proc = "${command}".execute(null, dir)
		proc.waitForProcessOutput(System.out, System.err)

	}

}
