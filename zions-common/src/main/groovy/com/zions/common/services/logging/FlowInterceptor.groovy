package com.zions.common.services.logging

import groovy.json.JsonBuilder
import org.codehaus.groovy.runtime.StringBufferWriter

/**
 * Trait to intercept method calls of an object and log entry/exit
 * @author z091182
 *
 */
trait FlowInterceptor implements Interceptor {
	
	boolean showData = false
	boolean logging = true
	
	/**
	 * Setup flow logging on a single object
	 * 
	 * @param obj - object instance to log
	 * @param logging - flag to perform logging
	 * @param showData - flag to show argument in json format
	 * @param c - closure with objects to be managed.
	 * @return none
	 */
	def flowLogging(def obj,  boolean logging = true, boolean showData = false, Closure c) {
		this.showData = showData
		this.logging = logging
		def proxy = ProxyMetaClass.getInstance(obj.class)
		proxy.interceptor = this
		def cMetaClass = obj.metaClass
		obj.metaClass =  proxy
		c.call()
		obj.metaClass = cMetaClass
		proxy.interceptor = null
//		proxy.use(closure)
	}
	
	/**
	 * Setup flow logging on a multiple objects
	 * 
	 * @param obj - list of object instances to log
	 * @param logging - flag to perform logging
	 * @param showData - flag to show argument in json format
	 * @param c - closure with objects to be managed.
	 * @return none
	 */
	def flowLogging(List<Object> objs,  boolean logging = true, boolean showData = false, Closure c) {
		def objMap = [:]
		this.showData = showData
		this.logging = logging
		objs.each {obj -> 
			def proxy = ProxyMetaClass.getInstance(obj.class)
			proxy.interceptor = this
			def cMetaClass = obj.metaClass
			objMap[obj] = cMetaClass
			obj.metaClass =  proxy
		}
		c.call()
		objMap.each { key, obj ->
			obj.metaClass = objMap[key]
		}
//		proxy.use(closure)
	}

	/* (non-Javadoc)
	 * @see groovy.lang.Interceptor#beforeInvoke(java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	Object beforeInvoke(def obj, String methodName, Object[] args) {
		boolean hasLogProp = false
		try {
		 def log = obj.log
		 hasLogProp = true
		} catch (e) {}
		if (hasLogProp && logging) {
			StringWriter sb = new StringWriter();
			PrintWriter pw = new PrintWriter(sb);
			String msg = "Entering ${methodName}("
			int l = args.length
			int i = 1
			args.each { arg -> 
				String type = arg.getClass().simpleName
				msg = "${msg} ${type}"
				if (i == l) {
					msg = "${msg} )"
					
				} else {
					msg = "${msg},"
				}
				i++
			}
			pw.println msg
			
			if (showData) {
				int k = 0
				args.each { arg ->
					
					if (arg instanceof Map) {
						String json = new JsonBuilder(arg).toPrettyString()
						pw << "arg${k}:  ${json}";
						pw.println('')
					}
					else {
						pw.println "arg${k}:  ${arg}"
						
					}
					k++
				}
			}
			//msg = "${msg} )"
			String msgOut = sb.toString()
			obj.log.info(msgOut)
			sb.close()
			
		}
	}
	
	/* (non-Javadoc)
	 * @see groovy.lang.Interceptor#afterInvoke(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	Object afterInvoke(Object obj, String methodName, Object[] args, Object result) {
		boolean hasLogProp = false
		try {
		 def log = obj.log
		 hasLogProp = true
		} catch (e) {}
		if (hasLogProp && logging) {
			StringWriter sb = new StringWriter();
			PrintWriter pw = new PrintWriter(sb);
			String msg = "Leaving ${methodName}("
			int l = args.length
			int i = 1
			args.each { arg -> 
				String type = arg.getClass().simpleName
				msg = "${msg} ${type}"
				if (i == l) {
					msg = "${msg} )"
					
				} else {
					msg = "${msg},"
				}
				i++
			}
			//msg = "${msg} )"
			
			pw.println msg
			if (showData && result) {
				pw << "Result ${methodName}:  ${result}"
				pw.println('')
			}
			String msgOut = sb.toString()
			obj.log.info(msgOut)
			sb.close()
		} 
		return result
	}
	boolean doInvoke() { true }
}    

