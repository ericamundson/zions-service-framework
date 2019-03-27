package com.zions.common.services.cli.action

import com.zions.common.services.logging.FlowInterceptor
import org.springframework.boot.ApplicationArguments

/**
 * Main interface to implement for command line interaction.
 * 
 * @author z091182
 * TODO: Add api to implement output of documentation of command line.
 */
trait CliAction implements FlowInterceptor {
	/**
	 * Execute command line with provided arguments
	 * @param args
	 * @return
	 */
	abstract def execute(ApplicationArguments args);
	/**
	 * Validate command line arguments
	 * @param args
	 * @return
	 * @throws Exception
	 */
	abstract def validate(ApplicationArguments args) throws Exception;
}
