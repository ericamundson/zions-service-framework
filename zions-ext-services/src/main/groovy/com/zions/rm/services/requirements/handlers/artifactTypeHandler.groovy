package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class artifactTypeHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return "${value}";
	}

}
