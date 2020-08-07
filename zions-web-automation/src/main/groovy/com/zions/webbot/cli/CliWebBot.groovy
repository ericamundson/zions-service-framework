package com.zions.webbot.cli

import com.zions.common.services.logging.FlowInterceptor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait

import org.springframework.boot.ApplicationArguments

/**
 * Main interface to implement for command line interaction.
 * 
 * @author z091182
 */
interface CliWebBot {
	/**
	 * Execute command line with provided arguments
	 * @param args
	 * @return
	 */
	def execute(ApplicationArguments args, WebDriver driver, WebDriverWait wait);
	/**
	 * Validate command line arguments
	 * @param args
	 * @return
	 * @throws Exception
	 */
	def validate(ApplicationArguments args) throws Exception;
}
