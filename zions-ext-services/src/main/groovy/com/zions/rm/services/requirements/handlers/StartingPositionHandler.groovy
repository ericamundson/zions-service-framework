package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class StartingPositionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Starting Position'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return stringToNumber(val, 'StartingPositionHandler')
	}

}
