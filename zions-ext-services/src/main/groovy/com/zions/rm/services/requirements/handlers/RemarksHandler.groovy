package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class RemarksHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Remarks'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
