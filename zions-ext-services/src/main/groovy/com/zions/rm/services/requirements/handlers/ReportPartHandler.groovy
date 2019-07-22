package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ReportPartHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Report Part'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
