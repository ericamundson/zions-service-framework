package com.zions.rm.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService

@Component
class WhereUsedDocHandler extends RmBaseAttributeHandler {
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	@Override
	public String getFieldName() {
		
		return 'Where Used'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// Get base document name from the first containing module name
		def whereUsedList = cacheManagementService.getFromCache(itemData.getCacheID(), 'whereUsedData')
		if (whereUsedList != null) {
			String moduleName = "${whereUsedList[0].name}"
			String docName
			def ndxFirstBlank = moduleName.indexOf(' ')
			if (ndxFirstBlank > 0) {
				docName = moduleName.substring(0,ndxFirstBlank)
			}
			else {
				docName = moduleName
			}
			return docName
		}

		return null
	}

}
