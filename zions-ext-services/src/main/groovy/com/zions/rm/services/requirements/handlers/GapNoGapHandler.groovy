package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class GapNoGapHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Gap/NoGap'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
