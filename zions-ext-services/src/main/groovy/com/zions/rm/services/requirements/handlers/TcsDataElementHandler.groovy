package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsDataElementHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Data Element'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
