package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import groovy.json.JsonSlurper
import org.springframework.stereotype.Component
import org.apache.commons.lang.RandomStringUtils

@Component
class QuoteGenerator implements Generator {
	
	def quotes 
	def length
	Random r = new Random()

	public QuoteGenerator() {
		URL url = this.getClass().getResource('/quotes.json')
		File template = new File(url.file)
		quotes = new JsonSlurper().parse(template)
		length = quotes.size()
	}
	
	@Override
	public Object gen() {
		int i = r.nextInt(length)
		
		return quotes[i].quoteText
	}

}
