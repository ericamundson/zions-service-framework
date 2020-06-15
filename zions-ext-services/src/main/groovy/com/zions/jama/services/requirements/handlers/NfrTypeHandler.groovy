package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NfrTypeHandler extends RmBaseAttributeHandler {
	def nfrTypeMap = [170904:'Compliance',180495:'Capacity',170905:'Security',170906:'Performance',180497:'Hardware and Software Requirements',180493:'Availability',180494:'Disaster Recovery',180496:'Other']
	@Override
	public String getFieldName() {
		
		return 'nfr_type$94010'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value) {
			return nfrTypeMap[value]
		} else {
			return null
		}
	}

}
