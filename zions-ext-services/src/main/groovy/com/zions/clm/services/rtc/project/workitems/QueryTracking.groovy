package com.zions.clm.services.rtc.project.workitems

import com.zions.common.services.cacheaspect.CacheRequired
import com.zions.common.services.cacheaspect.CacheWData
import groovy.transform.Canonical
import groovy.xml.XmlUtil

@Canonical
class QueryTracking implements CacheWData {
	String data
	
	void doData(def result) {
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		return new XmlSlurper().parseText(data)
	}

}
