package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class MandatoryHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Mandatory'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val == 'M') {
			val = 'Mandatory'
		}
		else if (val == 'O') {
			val = 'Optional'
		}
		else if (val == 'C') {
			val = 'Conditional'
		}
		else if (val == 'MU') {
			val = 'Mandatory Update'
		}
		else if (val == 'OU') {
			val = 'Optional Update'
		}
		else if (val == 'CU') {
			val = 'Conditional Update'
		}
		else {
			return null
		}
		return val
	}

}
