package com.zions.rm.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService

@Component
class WhereUsedHandler extends RmBaseAttributeHandler {
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Where Used'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		def whereUsedHtml = null
		
		def whereUsedList = cacheManagementService.getFromCache(itemData.getCacheID(), 'whereUsedData')
		if (whereUsedList != null) {
			whereUsedHtml = '<div>'
			whereUsedList.each { ref ->
				whereUsedHtml = whereUsedHtml + "<a href=${ref.itemIdRelated}>${ref.type}</a><br>"
			}			
			whereUsedHtml = whereUsedHtml + '</div>'
			
		}

		return whereUsedHtml
	}

}
