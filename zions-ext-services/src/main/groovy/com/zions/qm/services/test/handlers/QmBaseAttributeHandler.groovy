package com.zions.qm.services.test.handlers

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

abstract class QmBaseAttributeHandler implements IFieldHandler {

	public Object execute(Object data) {
		def itemData = data.qmItemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		String name = getQmFieldName()
		String aValue = itemData."${name}".text()
		aValue = formatValue(aValue, itemData)
		if (aValue == null) {
			return null
		}

		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: aValue]
		if (wiCache != null) {
			def cVal = wiCache.fields."${fieldMap.target}"
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	
	abstract String getQmFieldName()
	
	abstract String formatValue(String val, def itemData)

}
