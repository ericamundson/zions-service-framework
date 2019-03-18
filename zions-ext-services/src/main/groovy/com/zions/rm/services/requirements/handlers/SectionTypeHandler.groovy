package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SectionTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "${value}"
		if (outVal == 'Compliance Requirement') {
			outVal = 'Compliance'
		}

		return outVal;
	}

}