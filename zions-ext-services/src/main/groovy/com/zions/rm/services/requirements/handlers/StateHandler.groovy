package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class StateHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		// Handle cleanup via the DELETE IN ADO Release value - set status to Closed
		String releaseVals = itemData.getAttribute('Release')
		if (releaseVals.indexOf('DELETE IN ADO') > -1) {
			return 'Closed'
		}
		else if (value == 'TCS Reviewed' || value == 'TCS Reviewed with Changes' || value == 'Zions Reviewed' ||
			value == 'Zions Reviewed with Changes' || value == 'Large Team Review w/ Changes' || value == 'Parked' ||
			value == 'Approved' || value == 'Solution Approved' || value == 'Submit for Program Approval' || 
			value == 'Ready for Stakeholder Review') {
			return 'Active'
		}
		else if (value == 'Duplicate' || value == 'Delete' || value == 'Rejected' || value == 'Deprecated' || value == 'Stakeholder Approved' || value == 'Program Approved') {
			return 'Closed'
		}
		else {
			return 'New'
		}
	}


}
