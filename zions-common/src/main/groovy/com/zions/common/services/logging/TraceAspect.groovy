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

@Aspect
@Configuration
@Slf4j
@EnableAspectJAutoProxy
public class TraceAspect {


	/*@Around("execution(* com.journaldev.spring.model.Employee.getName())")*/ 
	@Around('@annotation(com.zions.common.services.logging.Traceable)')
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger ilog = log
		def obj = joinPoint.obj
		try {
			obj.log.isDebugEnabled()
			ilog = obj.log
		} catch (e) {}
		if (ilog.isDebugEnabled()) {
			ilog.debug("Entering:  ${sig}")
		}
	    Object proceed = joinPoint.proceed();
		if (ilog.isDebugEnabled()) {
			ilog.debug("Leaving:  ${sig}")
		}

	    return proceed;
	 }
	}
