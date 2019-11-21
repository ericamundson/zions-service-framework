package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class SequenceNumberHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Sequence Number'
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
		    return stringToNumber(value, 'SequenceNumberHandlerHandler')
		}
		return value
	}

}
