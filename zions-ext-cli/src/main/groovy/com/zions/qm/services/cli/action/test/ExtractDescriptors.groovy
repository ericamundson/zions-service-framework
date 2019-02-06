package com.zions.qm.services.cli.action.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.zions.common.services.cli.action.CliAction;
import com.zions.qm.services.test.ClmTestManagementService
import groovy.json.JsonBuilder

@Component
public class ExtractDescriptors implements CliAction {
	
	@Autowired
	ClmTestManagementService clmTestManagementService

	@Override
	public Object execute(ApplicationArguments data) {
		String[] types = ['Test Case', 'Test Suite', 'Test Plan']
		String project = data.getOptionValues('clm.projectArea')[0]
		String tfsAreaPath = data.getOptionValues('tfs.areaPath')[0]
		types.each { type ->
			def outDesc = []
			def cats = clmTestManagementService.getCategories(project, type)
			this.addCatDescriptors(cats, outDesc, tfsAreaPath) 
			def ca = clmTestManagementService.getCustomAttributes(project, type)
			addCADescriptors(ca, outDesc, tfsAreaPath)
			String fName = type.replace(' ', '')
			File desc = new File("${fName}_Descriptor.json")
			def os = desc.newDataOutputStream()
			os << new JsonBuilder(outDesc).toPrettyString()
			os.close()
		}
		return null;
	}
	
	def addCADescriptors(ca, outDesc, tfsAreaPath) {
		def stuff = null
		ca.'soapenv:Body'.response.returnValue.values.each { item ->
			if (!item.archived) {
				def caItem = [name: item.identifier, displayName: item.name, attributeType: 'Text', areaPathsFilter: [tfsAreaPath], enumValues:[]]
				outDesc.add(caItem)
			}
		}
	}
	
	def addCatDescriptors(cats, outDesc, tfsAreaPath) {
		cats.'soapenv:Body'.response.returnValue.values.each { cat ->
			if (!cat.archived) {
				String type = 'Enumeration'
				if (cat.multiSelectable) {
					type = 'Multiselect'
				}
				String displayName = "${cat.name}"
				String name = displayName.replace(' ', '_')
				def catItem = [name: name, displayName: displayName, attributeType: type, areaPathsFilter: [tfsAreaPath], enumValues: []]
				cat.categories.each { val ->
					if (!val.archived) {
						catItem.enumValues.add(val.name)
					}
				}
				outDesc.add(catItem)
			}
		}
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}
