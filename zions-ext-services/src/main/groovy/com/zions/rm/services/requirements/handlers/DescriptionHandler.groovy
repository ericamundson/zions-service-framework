package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DescriptionHandler extends RmBaseAttributeHandler {
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		// TO DO:  strip out all namespace stuff from html
		String outVal = "${value}".replace("<h:div xmlns:h='http://www.w3.org/1999/xhtml'>",'<div>').replace('<h:','<').replace('</h:','</')
		if (value == null || value.length() == 0) return null
		return outVal;
	}

}
