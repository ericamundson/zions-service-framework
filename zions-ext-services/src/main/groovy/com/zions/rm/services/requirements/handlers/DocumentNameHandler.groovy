package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DocumentNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Document Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
