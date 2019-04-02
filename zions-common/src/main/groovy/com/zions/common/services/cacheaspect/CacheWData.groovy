package com.zions.common.services.cacheaspect

trait CacheWData implements CacheRequired {
	abstract void doData(def result)
	abstract def dataValue()
}
