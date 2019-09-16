package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsStartPositionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Start Position'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val) {
			// Validate numeric value
			try {
				Integer seq = Integer.parseInt(val);
			} catch (NumberFormatException | NullPointerException nfe) {
				throw new Exception("TcsStartPositionFromHandler threw exception, invalid number: $val")
				return null;
			}
		}
		return val
	}

}
