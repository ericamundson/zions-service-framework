package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SourceNameHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'source$93997'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return value
	}

}
