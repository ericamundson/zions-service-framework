package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NameHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'title'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "${value}"
		if (value.length() > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		return outVal;
	}

}
