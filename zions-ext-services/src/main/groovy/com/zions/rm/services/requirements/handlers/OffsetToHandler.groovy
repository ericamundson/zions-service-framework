package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class OffsetToHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Offset To'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val) {
			if (val == '#VALUE!') {
				return ''
			}
			else {
				// Validate numeric value
			    try {
			        Integer seq = Integer.parseInt(val);
			    } catch (NumberFormatException | NullPointerException nfe) {
					throw new Exception("OffsetToHandler threw exception, invalid number: $val")
			        return null;
			    }
			}
		}
		return val	
	}

}
