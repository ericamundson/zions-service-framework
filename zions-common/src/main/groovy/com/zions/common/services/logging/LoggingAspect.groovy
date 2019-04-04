package com.zions.common.services.logging

import groovy.util.logging.Slf4j
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration

@Aspect
@Slf4j

public class LoggingAspect {

/* Created by Michael Angelastro
 * 03/27/2019 for Zions Service Framework Logging
 * 
 * This is aLogging Aspect for the Loggable annotation that calculates method runtimes
 * for all methods under classes annotated with @Loggable*/

	/*@Around("execution(* com.journaldev.spring.model.Employee.getName())")*/ 
	@Around('execution (* *(..)) && @within(com.zions.common.services.logging.Loggable)')
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
	    long start = System.currentTimeMillis();
	 
	    Object proceed = joinPoint.proceed();
	 
	    long executionTime = System.currentTimeMillis() - start;
	 
	    log.info("${joinPoint.getSignature()} executed in ${executionTime}ms");
	    return proceed;
	 }
	}
