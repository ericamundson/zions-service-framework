package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SectionIDHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Section ID'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
