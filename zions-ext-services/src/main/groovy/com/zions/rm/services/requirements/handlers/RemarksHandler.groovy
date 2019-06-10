package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class RemarksHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Remarks'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
