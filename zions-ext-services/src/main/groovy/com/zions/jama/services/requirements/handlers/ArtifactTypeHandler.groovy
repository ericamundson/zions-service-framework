package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ArtifactTypeHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return "${value}";
	}

}
