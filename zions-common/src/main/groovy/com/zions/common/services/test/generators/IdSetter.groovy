package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

@Component
class IdSetter implements Generator {
	public String id
	@Override
	public Object gen() {
		return id
	}

}
