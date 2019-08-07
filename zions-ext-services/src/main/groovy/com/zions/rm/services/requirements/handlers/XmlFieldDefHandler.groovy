package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class XmlFieldDefHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'XML Field Def'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
