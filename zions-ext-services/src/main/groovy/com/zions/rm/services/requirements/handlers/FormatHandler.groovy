package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FormatHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Format'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
