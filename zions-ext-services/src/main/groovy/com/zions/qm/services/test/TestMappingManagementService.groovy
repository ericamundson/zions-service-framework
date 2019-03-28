package com.zions.qm.services.test

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@Slf4j
class TestMappingManagementService {
	@Autowired
	@Value('${test.mapping.file:}')
	String testMappingFileName
	
	def mappingDataInfo = []
	
	public TestMappingManagementService() {
		
	}
	
	def getMappingData() {
		if (mappingDataInfo.size() > 0) {
			return mappingDataInfo
		}
		def xmlMappingData = new XmlSlurper().parse(new File(testMappingFileName))
		xmlMappingData.wit.each { tType ->
			def map = [source: tType.@source, target: tType.@target, fields: [], excluded: []]
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
			tType.excluded.field.each { field -> 
				String fName = "${field.@name}"
				map.excluded.add(fName) 
			}
			this.mappingDataInfo.add(map)
		}
		//Test data
		return mappingDataInfo
	}
}
