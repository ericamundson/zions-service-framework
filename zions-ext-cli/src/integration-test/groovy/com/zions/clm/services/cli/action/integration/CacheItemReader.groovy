package com.zions.clm.services.cli.action.integration

import groovy.json.JsonSlurper

class CacheItemReader {
	
	static void main(String[] args) {
		File toRead = new File(args[0])
		def cacheItem = new JsonSlurper().parse(toRead)
		String json = cacheItem.json
		def data = new JsonSlurper().parseText(json)
		def xml = data.data
		File out = new File(args[1])
		def os = out.newDataOutputStream()
		os << xml
		os.close()
	}

}
