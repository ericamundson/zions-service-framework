package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class IofLogicHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'IOF Logic'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return toHtml(val)
	}

}
