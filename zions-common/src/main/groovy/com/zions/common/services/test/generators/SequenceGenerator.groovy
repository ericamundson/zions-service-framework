package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

@Component
class SequenceGenerator implements Generator {
	int start = 10000
	@Override
	public Object gen() {
		// TODO Auto-generated method stub
		
		return start++
	}

}
