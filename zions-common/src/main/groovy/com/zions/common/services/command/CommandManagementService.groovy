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
	
	/**
	 * Handle command line call.
	 * 
	 * @param command - syntax to execute
	 * @param dir - working directory
	 * @return - no real return.
	 */
	def executeCommand(String command, File dir) {
		def proc = "${command}".execute(null, dir)
		proc.waitForProcessOutput(System.out, System.err)

	}

}
