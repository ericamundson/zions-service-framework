package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiTooltipHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'UI Tooltip'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
