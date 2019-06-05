package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiDefaultValueHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Default Value'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
