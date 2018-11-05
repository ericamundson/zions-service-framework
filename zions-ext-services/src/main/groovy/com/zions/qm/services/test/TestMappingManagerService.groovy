package com.zions.qm.services.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TestMappingManagerService {
	@Autowired
	@Value('test.mapping.file')
	String testMappingFileName
	
	def mappingData = []
	
	public TestMappingManagerService() {
		
	}
	
	def getMappingData() {
		if (mappingData.size() > 0) {
			return mappingData
		}
		def xmlMappingData = new XmlSlurper().parse(new File(testMappingFileName))
		xmlMappingData.wit.each { tType ->
			def map = [source: tType.@source, target: tType.@target, fields: []]
			tType.field.each { field ->
				def ofield = [handler: field.@source, target: field.@target, defaultValue: '', values:[]]
				field.'value'.each { aValue ->
					def oValue = [source: aValue.@source, target: aValue.@target]
					ofield.values.add(oValue) 
				}
				field.defaultvalue.each { dValue ->
					ofield.defaultValue = dValue.@target
				}
				map.fields.add(ofield)
			}
			mappingData.add(map)
		}
		return mappingData
	}
}
