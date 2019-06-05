package com.zions.common.services.cacheaspect

import com.zions.common.services.cache.ICacheManagementService

import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired

/**
 * This trait can be added to any class anonymous or not to enable ability to surround a method call with ability 
 * to cache return of method with any given ID and Timestamp.
 * 
 * @author z091182
 *
 */
@Slf4j
trait CacheInterceptor implements Interceptor {
	
	
	String id
	
	Date timestamp
	
	boolean doRun = true
	
	ICacheManagementService cacheManagementService
	
	String eType
	
	Class eTypeClass
	CacheWData cacheData
	
	List<String> methods
	
	boolean ignore
	
	/**
	 * This call sets up the ability to cache any specified object (obj) with the ability to cache return 
	 * from method calls within closure.
	 * 
	 * @param obj - object to enable caching.
	 * @param id - ID of object to cache
	 * @param timestamp - Timestamp to check stale cache
	 * @param managedType - Must inherit from CacheWData abstract methods must serialize method returns.
	 * @param methods - method filter.
	 * @param ignore - turn off and on capability
	 * @param closure - closure to encapsulate calls to cache.
	 */
	void provideCaching(def obj, String id, Date timestamp, Class managedType, List<String> methods= null, boolean ignore = false, Closure closure) {
		this.id = id
		eType = managedType.simpleName
		this.eTypeClass = managedType
		this.timestamp = timestamp
		this.ignore = ignore
		cacheManagementService = obj.properties['cacheManagementService']
		doRun = true
		this.methods = methods
		def proxy = ProxyMetaClass.getInstance(obj.class)
		proxy.interceptor = this
//		proxy.use closure
		def cMetaClass = obj.metaClass
		obj.metaClass =  proxy
		closure.call()
		obj.metaClass = cMetaClass
		proxy.interceptor = null
			
	}
	
	@Override
	public Object beforeInvoke(Object object, String methodName, Object[] arguments) {
		if (!ignore && (!methods || methods.contains(methodName))) {
			doRun = false
			cacheData = null
			if (!(CacheWData.class.isAssignableFrom(eTypeClass)) || !cacheManagementService) {
				doRun = true
				return
			}
			def cacheRaw = cacheManagementService.getFromCache(id, eType)
			if (cacheRaw) {
				cacheData = eTypeClass.newInstance(cacheRaw)
			}
			if (!cacheData) {
				doRun = true
				return
			} else if (cacheData.timestampValue().time < timestamp.time) {
				cacheData = null
				doRun = true
				return
			}
			//log.trace("beforeInvoke is returning cached data")
			return cacheData.dataValue()
		}
	}

	@Override
	public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
		if (!ignore && (!methods || methods.contains(methodName))) {
			if (!cacheData) {
				CacheWData data = this.eTypeClass.newInstance()
				data.doData(result)
				//log.trace("afterInvoke is saving serialized data to cache")
				cacheManagementService.saveToCache(data, id, eType)
			}
		}
		return result
	}

	@Override
	public boolean doInvoke() {
		return doRun
	}

}
