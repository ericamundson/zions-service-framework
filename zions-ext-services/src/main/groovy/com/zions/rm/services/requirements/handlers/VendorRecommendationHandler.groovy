package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class VendorRecommendationHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'TCS Recommendation'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val == null) {
			return val
		} else {
			return "<div>${val}</div>"
		}
	}

}
