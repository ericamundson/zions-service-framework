package com.zions.common.services.logging

import groovy.util.logging.Slf4j
import java.util.logging.Logger
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration

@Aspect
@Slf4j

public class LoggingAspect {

/* Created by Michael Angelastro
 * 05/29/2019 for Zions Service Framework Logging
 * 
 * This is aLogging Aspect for the Loggable annotation that calculates method runtimes
 * for all methods under classes annotated with @Loggable*/

	@Around('execution (* *(..)) && @within(com.zions.common.services.logging.Loggable)')
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		
		def obj = joinPoint.this
		Logger iLog = log
		long start = System.currentTimeMillis();
		Object proceed = joinPoint.proceed();
		long executionTime = System.currentTimeMillis() - start;
		
		try {
			
			/*First statement of try block attempts to test if log member exists on object.
			If it does, then iLog will get set to incoming object's loggers*/
			
				obj.log.isInfoEnabled()
				iLog = obj.log
			} catch (e) {}
	 
	    iLog.info("${joinPoint.getSignature()} executed in ${executionTime}ms");
	    return proceed;
	 }
	}
