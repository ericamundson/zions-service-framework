package com.zions.common.services.test.generators

import com.zions.common.services.test.Generator
import groovy.json.JsonSlurper
import org.springframework.stereotype.Component
import org.apache.commons.lang.RandomStringUtils

@Component
class QuoteGenerator implements Generator {
	
	def quotes 
	int length
	Random r = new Random()
	
	String defQuotes = '''
[{
"quoteText": "Genius is one percent inspiration and ninety-nine percent perspiration.",
"quoteAuthor": "Thomas Edison"
}, {
"quoteText": "You can observe a lot just by watching.",
"quoteAuthor": "Yogi Berra"
}, {
"quoteText": "A house divided against itself cannot stand.",
"quoteAuthor": "Abraham Lincoln"
}]
'''

	public QuoteGenerator() {
	}
	
	private def init() {
		if (length == 0) {
			try {
				URL url = this.getClass().getResource('/quotes.json')
				File template = new File(url.file)
				quotes = new JsonSlurper().parse(template)
				length = quotes.size()
			} catch (e) {
				quotes = new JsonSlurper().parseText(defQuotes)
				length = quotes.size()

			}
		}

	}
	
	@Override
	public Object gen() {
		init()
		int i = r.nextInt(length)
		
		return quotes[i].quoteText
	}

}
