package com.zions.rm.services.requirements.handlers

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

/**
 * Base class for producing work item field entries.
 * 
 * @author z091182
 * 
 * @startuml
 * 
 * abstract class RmBaseAttributeHandler {
 * + def execute(def data)
 * + <<abstract>> String getFieldName()
 * + <<abstract>> def formatValue(def val, def itemData)
 * }
 * 
 * IFieldHandler <|.. RmBaseAttributeHandler
 * 
 * @enduml
 * 
 *
 */
abstract class RmBaseAttributeHandler implements IFieldHandler {

	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		String name = getFieldName()
		def aValue = itemData."${name}".text()
		aValue = formatValue(aValue, itemData)
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
						if ("${fValue}" == "${aval.source}") {
							val = "${aval.target}"
							return
						}
					}
				}
				aValue = val
			}
		}

		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: aValue]
		if (wiCache != null) {
			String cVal = wiCache.fields."${fieldMap.target}"
			if ("${cVal}" == "${retVal.value}") {
				return null
			}


		}
		return retVal;
	}
	
	abstract String getFieldName()
	
	abstract def formatValue(def val, def itemData)

}
