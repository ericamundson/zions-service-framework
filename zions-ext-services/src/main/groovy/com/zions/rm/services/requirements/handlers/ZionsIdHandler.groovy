package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ZionsIdHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Zions ID'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
