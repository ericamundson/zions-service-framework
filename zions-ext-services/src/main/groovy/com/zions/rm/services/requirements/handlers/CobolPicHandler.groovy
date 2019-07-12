package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class CobolPicHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Cobol Pic'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
