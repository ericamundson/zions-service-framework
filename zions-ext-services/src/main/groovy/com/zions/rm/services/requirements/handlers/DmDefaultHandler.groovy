package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DmDefaultHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'DM Default'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}