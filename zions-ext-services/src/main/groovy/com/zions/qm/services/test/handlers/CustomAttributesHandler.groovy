package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CustomAttributesHandler extends QmBaseAttributeHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'none'
	}

	public def formatValue(def value, def data) {
		def outData = [];
		def itemData = data.itemData
		def catMap = [:]
		itemData.category.each { cat -> 
			String name = "${cat.@term}".replace(' ', '_')
			String avalue = "${cat.@value}"
			if (!catMap["${name}"]) {
				catMap["${name}"] = []
			}
			catMap["${name}"].push(avalue)
		}
		catMap.each { key, vals -> 
			def catData = [name: key, value: vals.join(';')]
			outData.push(catData)
		}
		itemData.customAttributes.customAttribute.each { att ->
			String name = "${att.identifier.text()}"
			String avalue = "${att.value.text()}"
			def caData = [name: name, value: avalue]
			outData.push(caData)
		}
		def out = [values:outData]
		return new JsonBuilder(out).toString();
	}

}
