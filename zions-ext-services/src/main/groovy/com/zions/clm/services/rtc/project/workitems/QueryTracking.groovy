package com.zions.clm.services.rtc.project.workitems

import com.zions.common.services.cacheaspect.CacheRequired
import com.zions.common.services.cacheaspect.CacheWData
import groovy.transform.Canonical
import groovy.xml.XmlUtil

/**
 * Bean to house work item query page results in cache.
 * This object gets serialized to cache store.
 * 
 * @author z091182
 *
 */
@Canonical
class QueryTracking implements CacheWData {
	String data
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cacheaspect.CacheWData#doData(java.lang.Object)
	 */
	void doData(def result) {
		data = new XmlUtil().serialize(result)
	}
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cacheaspect.CacheWData#dataValue()
	 */
	def dataValue() {
		return new XmlSlurper().parseText(data)
	}

}
