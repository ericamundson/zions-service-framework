package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

@Component
class BuildWITStarter implements CliAction {
	
	public ExtractCcmWIMetadata() {
	}
	
	def buildStarterXml(def inFile) {
		File fInFile = new File(inFile)
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)
		bXml.'witd:WITD'(application:'Work item type editor',
			version: '1.0',
			'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
			WORKITEMTYPE(name: 'general') {
				DESCRIPTION {
					'general work item starter'
				}
				FIELDS {
					fInFile.readLines().each { line ->
						def items = line.split("\\|")
						FIELD(name: "${items[0]}", refname: "RTC.${items[0]}", type: "${items[1]}".trim(), dimension: 'reportable') {
							HELPTEXT "${items[2]}"
						}
					}
			
				}
			}
		}
		println writer.toString()
		
	}

	public def execute(ApplicationArguments data) {
		String inFile = data.getOptionValues('in.file')[0]
		String outFile = data.getOptionValues('out.file')[0]
		def metadata = buildStarterXml(inFile)
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['in.file', 'out.file' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
