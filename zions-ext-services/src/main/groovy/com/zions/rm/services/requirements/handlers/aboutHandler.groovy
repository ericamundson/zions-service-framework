package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class aboutHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'about'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return "<div><a href=${value} target=\"_blank\">DNG Artifact</a></div>"
	}

}
