package com.zions.qm.services.test.handlers

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

@Component
class PlanBlankHandler implements IFieldHandler {
	
	public PlanBlankHandler() {}

	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		String aValue = ''
		String val = ''
		if (fieldMap.defaultValue != null) {
			val = "${fieldMap.defaultValue}"
		}
		aValue = val
		

		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: [name:aValue]]
		if (wiCache != null) {
			String type = "${itemMap.target}"
			if (type != 'Test Case') {
				def cVal = wiCache."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
			} else {
				String cVal = wiCache.fields."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
	
			}
		}
		return retVal;
	}
	

}
