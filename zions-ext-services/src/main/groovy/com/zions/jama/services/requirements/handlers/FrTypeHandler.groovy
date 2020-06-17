package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FrTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return 'Functional Requirement';
	}

}
