package com.zions.common.services.cacheaspect

import com.zions.common.services.cache.ICacheManagementService
import org.springframework.beans.factory.annotation.Autowired

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
		// TODO Auto-generated method stub
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
			
			return cacheData.dataValue()
		}
	}

	@Override
	public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
		if (!ignore && (!methods || methods.contains(methodName))) {
			if (!cacheData) {
				CacheWData data = this.eTypeClass.newInstance()
				data.doData(result)
				cacheManagementService.saveToCache(data, id, eType)
			}
		}
		return result
	}

	@Override
	public boolean doInvoke() {
		// TODO Auto-generated method stub
		return doRun
	}

}
