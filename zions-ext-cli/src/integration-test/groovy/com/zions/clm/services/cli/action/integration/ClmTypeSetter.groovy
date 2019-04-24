package com.zions.clm.services.cli.action.integration

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

@Component
class ClmTypeSetter implements Generator {
	String type
	@Override
	public Object gen() {
		return type
	}

}
