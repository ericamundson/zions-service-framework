package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

/**
 * Generate a sequenced integer.
 * 
 * @author z091182
 *
 */
@Component
class SequenceGenerator implements Generator {
	int start = 10000
	@Override
	public Object gen() {
		
		
		return start++
	}

}
