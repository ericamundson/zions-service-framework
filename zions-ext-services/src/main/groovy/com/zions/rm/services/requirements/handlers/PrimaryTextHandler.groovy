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
		if (val) {
			String description = removeNamespace("${val}")
			// Fix to special characters (Unicode from Word uploads) is now being handled in ClmRequirementsManagementService prior to xml parsing
			return description.replaceAll("&lt;",'<').replaceAll("&gt;",'>') //.replaceAll("[^\\p{ASCII}]", "")
		}
		else {
			return val
		}
	}

}
