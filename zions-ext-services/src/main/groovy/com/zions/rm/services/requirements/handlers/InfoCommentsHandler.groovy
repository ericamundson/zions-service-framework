package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class InfoCommentsHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Info Comments'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
