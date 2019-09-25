package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class StartingPositionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Starting Position'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val) {
			// Validate numeric value
			try {
				Integer seq = Integer.parseInt(val);
			} catch (NumberFormatException | NullPointerException nfe) {
				throw new Exception("StartingPositionHandler threw exception, invalid number: $val")
				return null;
			}
		}
		return val
	}

}
