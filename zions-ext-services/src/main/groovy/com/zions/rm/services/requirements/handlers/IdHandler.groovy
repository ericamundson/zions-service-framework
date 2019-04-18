package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class IdHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Identifier'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "DNG-${value}"
		return outVal;
	}

}
