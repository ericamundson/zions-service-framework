package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component
import org.apache.commons.lang.RandomStringUtils

/**
 * Generates a random UUID.
 * 
 * @author z091182
 *
 */
@Component
class UuidGenerator implements Generator {
	
	

	@Override
	public Object gen() {
		return UUID.randomUUID().toString()
	}

}
