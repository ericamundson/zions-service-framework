package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class StateHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == 'Draft' || value == 'Ready for Review' || value == 'Solution Approved') {
			return value
		}
		else if (value == 'TCS Reviewed' || value == 'TCS Reviewed with Changes' || value == 'Zions Reviewed' ||
			     value == 'Zions Reviewed with Changes' || value == 'Large Team Review w/ Changes' || value == 'Parked') {
			return 'Active'
		}
		else if (value == 'Duplicate' || value == 'Delete' || value == 'Rejected' || value == 'Deprecated' || value == 'Stakeholder Approved') {
			return 'Closed'
		}
		else {
			return null
		}
	}


}
