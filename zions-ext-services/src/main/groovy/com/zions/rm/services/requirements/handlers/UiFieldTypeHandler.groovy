package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiFieldTypeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'UI Field Type'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
