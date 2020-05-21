package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NfrTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'NFR Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return value;
	}

}
