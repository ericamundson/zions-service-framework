package com.zions.clm.services.rtc.project.workitems

import com.zions.common.services.cacheaspect.CacheRequired
import groovy.transform.Canonical
import groovy.xml.XmlUtil

@Canonical
class QueryTracking implements CacheRequired {
	String xml
	
	void doResult(def result) {
		xml = new XmlUtil().serialize(result)
	}
	
	def resultValue() {
		return new XmlSlurper().parseText(xml)
	}

}
