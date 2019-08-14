package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class S_NoHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'S.No.'
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
			// Validate numeric value
		    try {
		        Integer seq = Integer.parseInt(value);
		    } catch (NumberFormatException | NullPointerException nfe) {
				throw new Exception("SequenceNoHandler threw exception, invalid value for SequenceNo: $value")
		        return null;
		    }
		}
		return value
	}

}
