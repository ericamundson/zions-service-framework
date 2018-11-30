package com.zions.rm.services.requirements

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Load rm mapping file
 * 
 * Design<p/>
 * <img src="RequirementsMappingManagementService.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class RequirementsMappingManagementService {
 * ... data ...
 * @Autowired String requirementsMappingFileName 
 * get it's value from ${req.mapping.file} Spring property
 * ... behavior ...
 *  +def getMappingData()
 * }
 * note left: @Component
 @enduml
 *
 */
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
