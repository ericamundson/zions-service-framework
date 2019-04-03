package com.zions.common.services.cacheaspect

import com.zions.common.services.cache.ICacheManagementService

import groovy.json.JsonBuilder
import java.lang.reflect.Method
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cglib.core.Signature
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Aspect
@Configuration
class CacheAspect {
	
	public CacheAspect() {
		//println 'Cache aspect running'
	}
	
	@Autowired
	ICacheManagementService cacheManagementService

	@Around('@annotation(com.zions.common.services.cacheaspect.Cache)')
	public def around(ProceedingJoinPoint joinPoint) throws Throwable {
		def args = joinPoint.args
		def obj = joinPoint.this
		MethodSignature sig = joinPoint.getSignature()
		Method method = sig.method
		
		Cache cacheAnno = method.getAnnotation(Cache.class)
		Class eTypeClass = cacheAnno.elementType()
		String eType = cacheAnno.elementType().simpleName
		if (args.length < 2) {
			return joinPoint.proceed()
		}
		if (!(args[0] instanceof String)) {
			return joinPoint.proceed()
		}
		if (!(args[1] instanceof Date)) {
			return joinPoint.proceed()
		}
		if (!(CacheRequired.class.isAssignableFrom(eTypeClass))) {
			return joinPoint.proceed()
		}

		//Checkpoint checkpoint = checkpointManagementService.selectCheckpoint('update')
		
		String id = args[0]
		//sanitize id
		id = id.replace('\\W+', '')
		
		Date ts = args[1]
		
		//String moduleStr = "${module}"
		def retVal = cacheManagementService.getFromCache(id, eType)
		
		if (!retVal) {
			retVal = joinPoint.proceed()
			if (CacheWData.class.isAssignableFrom(eTypeClass)) {
				CacheWData data = eTypeClass.newInstance()
				data.doData(retVal)
				cacheManagementService.saveToCache(data, id, eType)
			} else {			
				cacheManagementService.saveToCache(retVal, id, eType)
			}
		} else {
			if (CacheWData.class.isAssignableFrom(eTypeClass)) {
				CacheWData data = eTypeClass.newInstance(retVal)
				retVal = data.dataValue()
				if (data.timeStamp.time < ts.time) {
					retVal = joinPoint.proceed()
					CacheWData indata = eTypeClass.newInstance()
					indata.doData(retVal)
					cacheManagementService.saveToCache(indata, id, eType)
				}
			} else if (retVal instanceof List) {
				List<CacheRequired> inItems = new ArrayList<CacheRequired>()
				retVal.each { map -> 
					def item = eTypeClass.newInstance(map)
					inItems.add(item)
				}
				retVal = inItems
				List<CacheRequired> lessThan = retVal.findAll { CacheRequired item ->
					(item.timestampValue().time < ts.time)
				}
				if (lessThan.size() > 0) {
					retVal = joinPoint.proceed()
					cacheManagementService.saveToCache(retVal, id, eType)	
				}
			} else {
				CacheRequired inItem = eTypeClass.newInstance(retVal)
				retVal = inItem
				if (inItem.timestampValue().time < ts.time) {
					retVal = joinPoint.proceed()
					cacheManagementService.saveToCache(retVal, id, eType)

				}
			}
		}
		
		
		return retVal
		
		
	}
}
