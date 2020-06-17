package com.zions.jama.services.requirements.handlers

import com.zions.rm.services.requirements.ClmRequirementsModule
import org.springframework.stereotype.Component

@Component
class DocumentTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return 'Business Requirements Document'
	}

}
