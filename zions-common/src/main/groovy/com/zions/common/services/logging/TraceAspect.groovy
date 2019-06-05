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
 * Setup to log class method entry/exit.  
 *  
 * @author z091182
 *
 */
@Aspect
@Configuration
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class TraceAspect {


	@Around('execution (* *(..)) && @within(com.zions.common.services.logging.Traceable)')
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger ilog = log
		Signature sig = joinPoint.signature
		String mName = sig.name
		if (ilog.isInfoEnabled()) {
			ilog.info("Entering:  ${sig}")
		}
	    Object proceed = joinPoint.proceed();
		if (ilog.isInfoEnabled()) {
			ilog.info("Leaving:  ${sig}")
		}

	    return proceed;
	 }
	}
