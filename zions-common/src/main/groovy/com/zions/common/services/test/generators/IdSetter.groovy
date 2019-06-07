package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

/**
 * Output a string replace with specified id.
 * 
 * @author z091182
 *
 */
@Component
class IdSetter implements Generator {
	public String id
	@Override
	public Object gen() {
		return id
	}

}
