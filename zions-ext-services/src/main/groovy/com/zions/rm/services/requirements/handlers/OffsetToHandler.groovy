package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class OffsetToHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Offset To'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		return val
	}

}
