package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SourceNameHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'sourceName'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return value
	}

}
