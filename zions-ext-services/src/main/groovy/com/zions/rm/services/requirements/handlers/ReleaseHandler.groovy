package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ReleaseHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Release'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		if (itemData.getID() == '1570366') {
			def i = 0
		}
		if (val == 'Not Assigned') {
			return null
		}
		else {
			if (val == 'Consumer Lending' || val == 'One DOT One' || val == 'One DOT Two - Not Used' ) {
				val = 'R1'
			}
			else if (val == 'Commercial & Construction Lending' || val == 'Two DOT One' || val == 'Two DOT Two - Not Used' ||
					val == 'Two DOT Five' || val == 'Two DOT Nine') {
				val = 'R2'
			}
			else if (val == 'Deposits') {
				val = 'R3'
			}
			return val
		}
	}

}
