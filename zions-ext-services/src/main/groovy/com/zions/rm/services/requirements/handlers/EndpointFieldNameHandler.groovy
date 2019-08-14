package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class EndpointFieldNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Endpoint Field Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
