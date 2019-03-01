package com.zions.qm.services.cli.action.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.zions.common.services.cli.action.CliAction;
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder

@Component
public class ExtractDescriptors implements CliAction {
	
	@Autowired
	ClmTestManagementService clmTestManagementService

	@Autowired
	ProcessTemplateService processTemplateService

	@Autowired
	TestMappingManagementService testMappingManagementService
		
	@Value('${holder.wit:Test Attributes}')
	String holderWit
	
	@Override
	public Object execute(ApplicationArguments data) {
		String[] types = ['Test Case', 'Test Suite', 'Test Plan']
		String project = data.getOptionValues('clm.projectArea')[0]
		String tfsAreaPath = data.getOptionValues('tfs.areaPath')[0]
		String tfsProject = data.getOptionValues('tfs.project')[0]
		def wit = processTemplateService.getWIT('', tfsProject, holderWit)
		def mapping = testMappingManagementService.mappingData
		def allDesc = []
		types.each { type ->
			def outDesc = []
			def excluded = []
			mapping.each { map ->
				String target = "${map.target}"
				if (target == type) {
					excluded = map.excluded
					return
				}
			}
			def cats = clmTestManagementService.getCategories(project, type)
			this.addCatDescriptors(cats, outDesc, tfsAreaPath, excluded) 
			def ca = clmTestManagementService.getCustomAttributes(project, type)
			addCADescriptors(ca, outDesc, tfsAreaPath, excluded)
			outDesc.each { allDesc << it }
			String fName = type.replace(' ', '')
			File desc = new File("${fName}_Descriptor.json")
			def os = desc.newDataOutputStream()
			os << new JsonBuilder(outDesc).toPrettyString()
			os.close()
		}
		def changes = getChanges(allDesc)
		changes.each { change ->
			processTemplateService.ensureWitField('', tfsProject, wit, change)
			
		}
		return null;
	}
	
	def getChanges(allDesc) {
		def changes = []
		int count = 0
		int section = 1
		allDesc.each { desc ->
			String refName = desc.fieldName
			String type = 'string'
			if (desc.attributeType == 'Numeric') {
				type = 'integer'
			} else if (desc.attributeType == 'RichText') {
				type = 'html'
			} else if (desc.attributeType == 'DatePicker') {
				type = 'dateTime'
			}
			def groupName = 'Attributes'
			if (section > 1) {
				groupName = "Attributes${section}"
				
			}
			if ("${type}" == 'html') {
				groupName = "${desc.displayName}"
			}
			String displayName = desc.displayName.replaceAll("[^A-Za-z0-9 ]", "");
			def cField = [name:displayName, refName:refName, type:type, helpText: "RQM ${desc.name}" , page: 'Attributes', section: "Section${section}", group: "${groupName} Fields",suggestedValues:[]]
			changes.push(cField)
			count++
			if ((count % 15) == 0) {
				section++
			}
		}
		return changes
	}
	
	def addCADescriptors(ca, outDesc, tfsAreaPath, excluded) {
		def stuff = null
		ca.'soapenv:Body'.response.returnValue.values.each { item ->
			if (!item.archived) {
				if (!excluded.contains(item.identifier)) {
					def name = item.name.replaceAll("[^A-Za-z0-9 ]", "");
					name = name.replace(' ', '_')
					def fieldName = "Custom.Test${toCamelCase(name)}"
					def caItem = [name: item.identifier, displayName: item.name, fieldName: fieldName, attributeType: 'Text', areaPathsFilter: [tfsAreaPath], enumValues:[]]
					outDesc.add(caItem)
				}
			}
		}
	}
	
	def addCatDescriptors(cats, outDesc, tfsAreaPath, excluded) {
		cats.'soapenv:Body'.response.returnValue.values.each { cat ->
			if (!cat.archived) {
				String type = 'Enumeration'
				if (cat.multiSelectable) {
					type = 'Multiselect'
				}
				String displayName = "${cat.name}"
				String name = displayName.replace(' ', '_')
				if (!excluded.contains(name)) {
					String fieldName = "Custom.Test${toCamelCase(name)}"
					if (displayName == 'Automation Status') {
						displayName = "RQM ${displayName}"
					}
					def catItem = [name: name, displayName: displayName, fieldName: fieldName, attributeType: type, areaPathsFilter: [tfsAreaPath], enumValues: []]
					cat.categories.each { val ->
						if (!val.archived) {
							catItem.enumValues.add(val.name)
						}
					}
					outDesc.add(catItem)
				}
			}
		}
	}
	
	static String toCamelCase( String text) {
		text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
		return text
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'tfs.url', 'tfs.user', 'tfs.token', 'tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}
