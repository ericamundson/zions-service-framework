package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class EsbCopybookPicSizeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'ESB Copybook PIC Size'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
