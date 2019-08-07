package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class RegulationNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Regulation Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
