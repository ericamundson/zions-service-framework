package com.zions.common.services.logging

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration

@Aspect
@Configuration
public class LoggingAspect {

/* Created by Michael Angelastro
 * 03/27/2019 for Zions Service Framework Logging
 * 
 * This is aLogging Aspect for the Loggable annotation that calculates method runtimes
 * for all methods under classes annotated with @Loggable*/

	/*@Around("execution(* com.journaldev.spring.model.Employee.getName())")*/
	@Around('@annotation(com.zions.common.services.cacheaspect.Cache)') 
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
	    long start = System.currentTimeMillis();
	 
	    Object proceed = joinPoint.proceed();
	 
	    long executionTime = System.currentTimeMillis() - start;
	 
	    System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
	    return proceed;
	 }
	}
