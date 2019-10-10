package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SequenceNoHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Sequence No'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == null || value == '') {
			// Use generated value (will only exist when migrating modules)
			if (itemData.getTypeSeqNo()) {
				value = itemData.getTypeSeqNo()
			}
		}
		else {
		    return stringToNumber(value, 'SequenceNoHandlerHandler')
		}
		return value
	}

}
