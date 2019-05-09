package com.zions.qm.services.test.handlers

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

abstract class QmBaseIntegerAttributeHandler extends QmBaseAttributeHandler {
	
	boolean cacheCheck = true

	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		String name = getQmFieldName()
		def aValue = itemData."${name}".text()
		aValue = formatValue(aValue, data)
		if (aValue == null) {
			return null
		} else {
			if (aValue instanceof String) {
				String val = "${aValue}"
				if (fieldMap.defaultValue != null) {
					val = "${fieldMap.defaultValue}"
				}
				if (fieldMap.values.size() > 0) {
	
					fieldMap.values.each { aval ->
						if ("${aValue}" == "${aval.source}") {
							val = "${aval.target}"
							return
						}
					}
				}
				aValue = Integer.parseInt(val)
			} 
		}

		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: aValue]
		if (cacheCheck && wiCache != null) {
			String type = "${itemMap.target}"
			if (type == 'Test Case' || type.endsWith(' WI')) {
				String cVal = wiCache.fields."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
			} else {
				def cVal = wiCache."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
	
			}
		}
		return retVal;
	}
	
	abstract String getQmFieldName()
	
	abstract def formatValue(def val, def itemData)

}
