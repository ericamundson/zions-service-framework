package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TransactionCodeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Transaction Code'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
