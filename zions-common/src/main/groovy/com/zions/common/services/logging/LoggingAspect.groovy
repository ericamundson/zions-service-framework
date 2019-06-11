package com.zions.common.services.logging

import groovy.util.logging.Slf4j
import java.util.logging.Logger
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration

/**
 * Handles method timing logs.
 *
 * @author Michael Angelastro
 *
 */
@Aspect
@Slf4j
public class LoggingAspect {

	/* Created by
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
		Object proceed = joinPoint.proceed();
		Logger iLog = log

		
		try {
			
			/*First statement of try block attempts to test if log member exists on object.
			If it does, then iLog will get set to incoming object's logger*/
			
			if(log.isInfoEnabled()){
			iLog = obj.log
			iLog.info("${joinPoint.getSignature()} executed in ${executionTime}ms");
				
			}else{
		   //If log member does not exist use aspect logging code
			Logger ilog = log
			   long start = System.currentTimeMillis();
			long executionTime = System.currentTimeMillis() - start;
			log.info("${joinPoint.getSignature()} executed in ${executionTime}ms");

	}
			} catch (e) {
				e.printStackTrace();
			}
			
			//log.info("${joinPoint.getSignature()} executed in ${executionTime}ms");
			return proceed;
		 }
		}

