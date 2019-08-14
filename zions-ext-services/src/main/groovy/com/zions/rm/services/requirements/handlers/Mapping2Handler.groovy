package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class Mapping2Handler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Mapping2'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
