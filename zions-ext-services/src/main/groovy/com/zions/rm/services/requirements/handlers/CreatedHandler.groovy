package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class CreatedHandler extends RmBaseAttributeHandler {
	private String fSimpleDateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'created'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return "${value}".replace('+0000','Z')
	}
}
