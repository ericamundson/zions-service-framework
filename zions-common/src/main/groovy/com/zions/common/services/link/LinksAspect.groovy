package com.zions.common.services.link

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.CheckpointManagementService

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
class LinksAspect {
	
	public LinksAspect() {
		println 'Links aspect running'
	}
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired(required=false)
	CheckpointManagementService checkpointManagementService

	@Around('@annotation(com.zions.common.services.link.Cache)')
	public def around(ProceedingJoinPoint joinPoint) throws Throwable {
		def args = joinPoint.args
		def obj = joinPoint.this
		MethodSignature sig = joinPoint.getSignature()
		Method method = sig.method
		Cache cacheAnno = method.getAnnotation(Cache.class)
		String moduleStr = cacheAnno.module().name()
		if (args.length < 3) {
			return joinPoint.proceed()
		}
		if (!(args[0] instanceof String)) {
			return joinPoint.proceed()
		}
		if (!(args[1] instanceof Date)) {
			return joinPoint.proceed()
		}

		//Checkpoint checkpoint = checkpointManagementService.selectCheckpoint('update')
		
		String id = args[0]
		Date ts = args[1]
		
		//String moduleStr = "${module}"
		
		List<LinkInfo> retVal = cacheManagementService.getFromCache(id, moduleStr)
		
		if (!retVal) {
			retVal = joinPoint.proceed()
			cacheManagementService.saveToCache(retVal, id, moduleStr)
		} else {
			List<LinkInfo> lessThan = retVal.findAll { LinkInfo info ->
				(info.timeStamp.time < ts.time)
			}
			if (lessThan.size() > 0) {
				retVal = joinPoint.proceed()
				cacheManagementService.saveToCache(retVal, id, moduleStr)	
			}
		}
		
		
		return retVal
		
		
	}
}
