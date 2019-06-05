package com.zions.common.services.test

import com.zions.common.services.cache.ICacheManagementService
import groovy.json.JsonBuilder
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
trait TestDataInterceptor implements Interceptor {
	
	
	
	boolean doRun = true
	
	String saveLocation
	
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
	void provideTestData(def obj, String saveLocation, boolean ignore = false, List<String> methods= null,  Closure closure) {
		doRun = true
		this.methods = methods
		this.ignore = ignore
		this.saveLocation = saveLocation
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
		
		doRun = true
	}

	@Override
	public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
		if (!ignore && (!methods || methods.contains(methodName))) {
			File saveDir = new File(saveLocation)
			File jFile = new File(saveDir, "${methodName}.json")
			try {
				String json = new JsonBuilder(result).toPrettyString()
				def os = jFile.newDataOutputStream()
				os << json
				os.close()
			} catch (e) {}
		}
		return result
	}

	@Override
	public boolean doInvoke() {
		
		return doRun
	}

}
