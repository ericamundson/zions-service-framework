package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ComplianceFinanceRelatedHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Compliance Related'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// First build compliance tag
		if (val == 'Yes') {
			val = 'COMPLIANCE'
		}
		else {
			val = ''
		}
		// Add Finance tag if needed
		String financeRelated = itemData.getAttribute('FinanceRelated')
		if (financeRelated == 'Yes') {
			if (val == null || val == '') {
				val = 'FINANCE'
			}
			else {
				val = val + ',FNANCE'
			}
		}
		return val

	}

}
