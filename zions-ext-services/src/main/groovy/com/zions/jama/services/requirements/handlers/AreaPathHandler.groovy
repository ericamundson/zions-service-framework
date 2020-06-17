package com.zions.jama.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AreaPathHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return null
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return itemData.areaPath
	}

}
