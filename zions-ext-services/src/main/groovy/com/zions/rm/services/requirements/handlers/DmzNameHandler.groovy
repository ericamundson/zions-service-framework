package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DmzNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'DMZ Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
