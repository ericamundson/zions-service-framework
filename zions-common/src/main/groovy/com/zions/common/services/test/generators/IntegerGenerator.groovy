package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

/**
 * Generate a random integer.
 * 
 * @author z091182
 *
 */
@Component
class IntegerGenerator implements Generator {

	@Override
	public Object gen() {
		return Math.abs(new Random().nextInt())
	}

}
