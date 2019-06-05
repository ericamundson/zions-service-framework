package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class VendorRecommendationHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Recommendation'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
