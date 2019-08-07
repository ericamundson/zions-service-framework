package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class PsaNumHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'PSA Document #'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
