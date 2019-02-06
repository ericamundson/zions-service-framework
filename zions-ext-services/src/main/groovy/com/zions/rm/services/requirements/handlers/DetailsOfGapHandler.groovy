package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DetailsOfGapHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Details of Gap'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		if (val == null) {
			return val
		} else {
			return "<div>${val}</div>"
		}
	}

}
