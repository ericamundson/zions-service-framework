package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DataAlignmentHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Data Alignment'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		return val
	}

}
