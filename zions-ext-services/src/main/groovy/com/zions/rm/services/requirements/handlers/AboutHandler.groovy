package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class AboutHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		
		return 'Base Artifact URI'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return "<div><a href=${value} target=\"_blank\">DNG Artifact ${itemData.getID()}</a></div>"
	}

}
