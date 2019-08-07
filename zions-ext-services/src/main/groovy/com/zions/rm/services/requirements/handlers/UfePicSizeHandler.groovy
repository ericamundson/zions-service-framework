package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UfePicSizeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'UFE PIC Size'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
