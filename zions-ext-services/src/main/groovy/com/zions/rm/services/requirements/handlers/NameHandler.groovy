package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NameHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		
		return 'title'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = null
		if (value != null && value != '') {
			outVal = "${value}"
		}
		else {
			outVal = '<blank title>'
		}
		// Truncate if too long
		if (outVal.length() > SIZE) {
			outVal = outVal.substring(0, SIZE-1)
		}
		return outVal;
	}

}
