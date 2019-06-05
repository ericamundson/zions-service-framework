package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class EntityTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "${value}"
		return outVal;
	}

}
