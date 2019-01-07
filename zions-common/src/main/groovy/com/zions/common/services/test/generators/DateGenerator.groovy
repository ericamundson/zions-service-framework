package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component
import org.apache.commons.lang.RandomStringUtils

@Component
class DateGenerator implements Generator {
	
	

	@Override
	public Object gen() {
		Date rDate = (new Date()..new Date(2014-1900,5,1)).toList().sort{Math.random()}[0]
		String sDate = rDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		return sDate
	}

}
