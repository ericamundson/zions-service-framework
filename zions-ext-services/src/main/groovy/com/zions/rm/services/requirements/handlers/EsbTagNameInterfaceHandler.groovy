package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class EsbTagNameInterfaceHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'ESB Tag Name/Interface'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
