package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class VendorIdHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Vendor ID Tracking #'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
