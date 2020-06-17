package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class IdHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'globalId'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "JAMA-${value}"
		return outVal;
	}

}
