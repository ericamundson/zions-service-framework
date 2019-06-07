package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DetailsOfGapHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Details of Gap'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
