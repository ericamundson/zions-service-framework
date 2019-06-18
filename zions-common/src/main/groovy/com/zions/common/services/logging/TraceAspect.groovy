package com.zions.common.services.logging

import groovy.util.logging.Slf4j
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Setup to log class method entry/exit. Any class marked with
 * @See com.zions.common.services.logging.Traceable annotation will utilize aspect advice.
 *  
 * @author z091182
 *
 */
@Aspect
@Configuration
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class TraceAspect {


	/**
	 * Around advice implementation for trace aspect.
	 * 
	 * @param joinPoint - join point data.
	 * @return - target's return
	 * @throws Throwable
	 */
	@Around('execution (* *(..)) && !execution(* *.getMetaClass()) && @within(com.zions.common.services.logging.Traceable)')
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger ilog = log
		Signature sig = joinPoint.signature
		def obj = joinPoint.this
		try {
			obj.log.isDebugEnabled()
			ilog = obj.log
		} catch (e) {}
		ilog.debug("Entering:  ${sig}")
		Object proceed = joinPoint.proceed();
		ilog.debug("Leaving:  ${sig}")

		return proceed;
	}
}
