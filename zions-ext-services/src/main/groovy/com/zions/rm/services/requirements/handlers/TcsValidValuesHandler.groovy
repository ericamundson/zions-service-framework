package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsValidValuesHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Valid Values'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
