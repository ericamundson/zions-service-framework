package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class BusinessLogicHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Business Logic'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
