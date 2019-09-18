package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ClosedReasonHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		// Handle cleanup via the DELETE IN ADO Release value - set status to Closed
		String releaseVals = itemData.getAttribute('Release')
		if (releaseVals.indexOf('DELETE IN ADO') > -1) {
			return 'Delete'
		}
		else if (value == 'Duplicate' || value == 'Delete' || value == 'Rejected' || value == 'Deprecated' ) {
			return value
		}
		else {
			return null
		}
	}


}
