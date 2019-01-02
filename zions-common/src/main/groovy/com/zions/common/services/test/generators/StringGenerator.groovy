package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component
import org.apache.commons.lang.RandomStringUtils

@Component
class StringGenerator implements Generator {
	
	Random r = new Random()

	@Override
	public Object gen() {
		String charset = (('A'..'Z') + ('0'..'9')).join()
		Integer length = r.nextInt(30) + 3
		String randomString = RandomStringUtils.random(length, charset.toCharArray())
		return randomString
	}

}
