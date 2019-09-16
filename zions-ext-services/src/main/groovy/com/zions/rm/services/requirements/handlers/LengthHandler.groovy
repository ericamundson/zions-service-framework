package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class LengthHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Length'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val) {
			// Validate numeric value
		    try {
		        Integer seq = Integer.parseInt(val);
		    } catch (NumberFormatException | NullPointerException nfe) {
				throw new Exception("LengthHandler threw exception, invalid number: $val")
		        return null;
		    }
		}
		return val
	}

}
