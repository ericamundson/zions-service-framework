package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class MigrationStatusHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Migration Status'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		return val
	}

}
