package com.zions.common.services.logging

import groovy.json.JsonBuilder
import org.codehaus.groovy.runtime.StringBufferWriter

trait FlowInterceptor implements Interceptor {
	
	boolean showData = false
	boolean logging = true
	
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

