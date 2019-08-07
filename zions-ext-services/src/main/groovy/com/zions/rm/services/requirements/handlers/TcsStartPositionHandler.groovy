package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsStartPositionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Start Position'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
