package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ActiveReasonHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == 'TCS Reviewed' || value == 'TCS Reviewed with Changes' || value == 'Zions Reviewed' ||
			     value == 'Zions Reviewed with Changes' || value == 'Large Team Review w/ Changes' || value == 'Parked' ||
				 value == 'Approved' || value == 'Solution Approved') {
			return value
		}
		else {
			return null
		}
	}


}
