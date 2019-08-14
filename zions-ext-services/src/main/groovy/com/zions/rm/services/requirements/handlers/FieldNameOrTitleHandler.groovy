package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FieldNameOrTitleHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Field Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		String outVal = null
		if (val == null || val == '') {
			// Use artifact name since Field Name is blank
			outVal = itemData.getTitle() 
			if (outVal == null || outVal == '') {
				outVal = 'No Title'
			}
		}
		else {
			outVal = val
		}
		// Truncate if too long
		return truncateStringField(outVal)
	}

}
