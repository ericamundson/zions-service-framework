package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SourceFieldNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Source Field Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
