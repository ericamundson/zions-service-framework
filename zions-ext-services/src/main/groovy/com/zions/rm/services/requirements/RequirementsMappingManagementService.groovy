package com.zions.rm.services.requirements

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RequirementsMappingManagementService {
	@Autowired
	@Value('${rm.mapping.file}')
	String requirementsMappingFileName
	
	def mappingDataInfo = []
	
	public RequirementsMappingManagementService() {
		
	}
	
	def getMappingData() {
		if (mappingDataInfo.size() > 0) {
			return mappingDataInfo
		}
		def xmlMappingData = new XmlSlurper().parse(new File(requirementsMappingFileName))
		xmlMappingData.wit.each { tType ->
			def map = [source: tType.@source, target: tType.@target, fields: []]
			tType.field.each { field ->
				def ofield = [source: field.@source, target: field.@target, defaultValue: null, values:[]]
				field.'value'.each { aValue ->
					def oValue = [source: aValue.@source, target: aValue.@target]
					ofield.values.add(oValue) 
				}
				field.defaultvalue.each { dValue ->
					ofield.defaultValue = dValue.@target
				}
				map.fields.add(ofield)
			}
			this.mappingDataInfo.add(map)
		}
		return mappingDataInfo
	}
}
