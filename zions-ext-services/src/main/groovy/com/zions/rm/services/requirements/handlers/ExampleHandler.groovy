package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ExampleHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Example'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return toHtml(val)
	}

}
