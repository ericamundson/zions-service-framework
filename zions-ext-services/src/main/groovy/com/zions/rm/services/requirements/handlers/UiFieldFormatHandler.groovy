package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiFieldFormatHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Field Format'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
