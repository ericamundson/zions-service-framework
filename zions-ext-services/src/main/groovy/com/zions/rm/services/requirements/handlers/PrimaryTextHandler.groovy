package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class PrimaryTextHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// strip out all namespace stuff from html
		String description = removeNamespace("${val}")
		return description.replaceAll("&lt;",'<').replaceAll("&gt;",'>').replaceAll("[^\\p{ASCII}]", "")
	}

}
