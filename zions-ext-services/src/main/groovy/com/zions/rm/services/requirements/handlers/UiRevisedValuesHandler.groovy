package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiRevisedValuesHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'UI Revised Field Values'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
