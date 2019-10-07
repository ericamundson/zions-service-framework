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
	static int MAX_STRING_SIZE = 255
	
	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		String name = getFieldName()
		def aValue = itemData.attributeMap."${name}"
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
	
	protected String toHtml(def value) {
		if (value == null || value == '' || value == ' ') {
			return '<div></div>'
		}
		else {
			return "<div>${value.replace('\r\n','<br>').replace('\n','<br>')}</div>"
		}
	}
	
	protected String removeNamespace(String value) {
		String description = value.replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
		description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')
		return description
	}
	protected String truncateStringField(String value) {
		if (value.length() > MAX_STRING_SIZE) {
			value = value.substring(0, MAX_STRING_SIZE-1)
		}
		return value;

	}
	protected def stringToNumber(def val, String handlerName) {
		if (val) {
			if (val == 'NA' || val == "YYYYMMDD") {
				return null
			}
			else {
				// Validate numeric value
				try {
					Integer seq = Integer.parseInt(val.trim().replaceAll(' bytes?','').replace(' - PACKED','').replace('PACKED ','').replace('x(','').replace('X(','').replace(')/MM/DD/YYYY','').replace(')',''))
				} catch (NumberFormatException | NullPointerException nfe) {
					throw new Exception("$handlerName threw exception, invalid number: $val")
					return null;
				}
			}
		}
	}
	
	abstract String getFieldName()
	
	abstract def formatValue(def val, def itemData)

}
