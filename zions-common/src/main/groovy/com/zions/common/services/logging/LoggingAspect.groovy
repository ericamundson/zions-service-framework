package com.zions.common.services.logging

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Handles method timing logs.  
 * 
 * @author Michael Angelastro
 *
 */
@Aspect
@Configuration
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class LoggingAspect {

	/* Created by Michael Angelastro
	 * 05/29/2019 for Zions Service Framework Logging
	 * 
	 * This is aLogging Aspect for the Loggable annotation that calculates method runtimes
	 * for all methods under classes annotated with @Loggable*/

	/**
	 * Logs execution time of method under aspect.
	 * 
	 * @param joinPoint - method under join
	 * @return actual return of method under join point.
	 * @throws Throwable
	 */
	@Around('execution (* *(..)) && @within(com.zions.common.services.logging.Loggable)')
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

		def obj = joinPoint.this
		Logger iLog = log
		long start = System.currentTimeMillis();
		Object proceed = joinPoint.proceed();
		long executionTime = System.currentTimeMillis() - start;

		try {

			/*First statement of try block attempts to test if log members exist on object.
			 If it does, then iLog will get set to incoming object's logger*/

			obj.log.isInfoEnabled()
			iLog = obj.log
		} catch (e) {}

		iLog.info("${joinPoint.getSignature()} executed in ${executionTime}ms");
		return proceed;
	}
}
