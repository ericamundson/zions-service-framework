package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class OffsetFromHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Offset From'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		return val
	}

}
