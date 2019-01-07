package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

@Component
class IntegerGenerator implements Generator {

	@Override
	public Object gen() {
		// TODO Auto-generated method stub
		return Math.abs(new Random().nextInt())
	}

}
