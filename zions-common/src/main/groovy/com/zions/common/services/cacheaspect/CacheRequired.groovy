package com.zions.common.services.cacheaspect

trait CacheRequired {
	String timeStamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	
//	void setTimeStamp(Date ts) {
//		atimeStamp = atimeStamp.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
//	}
	
	void setTimeStamp(String ts) {
		timeStamp = ts
	}

	String getTimeStamp() {
		return timeStamp
	}
	
	Date timestampValue() {
		return new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timeStamp)
	}
}
