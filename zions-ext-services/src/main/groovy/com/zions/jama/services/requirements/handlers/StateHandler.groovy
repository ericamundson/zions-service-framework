package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class StateHandler extends RmBaseAttributeHandler {
	@Override
	public String getFieldName() {
		
		return 'status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		// Handle cleanup via the DELETE IN ADO Release value - set status to Closed
		if (value) {
			value = stateMap[value]
			if (value == 'Draft') {
				return 'New'
			}
			else if (value == 'Approved' || value == 'Pending') {
				return 'Active'
			}
			else if (value == 'Completed' || value == 'Rejected') {
				return 'Closed'
			}
			else {
				return 'New'
			}
		}
		else {
			return null
		}
	}


}
