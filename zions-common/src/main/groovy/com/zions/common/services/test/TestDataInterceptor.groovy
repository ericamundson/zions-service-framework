package com.zions.common.services.test

import com.zions.common.services.cache.ICacheManagementService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired

/**
 * This trait can be added to any class anonymous or not to enable ability to surround a method call with ability 
 * to output return of method into locations so data can be used for test stubs.
 * 
 * @author z091182
 *
 */
@Slf4j
trait TestDataInterceptor implements Interceptor {
	
	
	
	boolean doRun = true
	
	String saveLocation
	
	List<String> methods
	
	String methodPrefix = ''
	
	boolean ignore
	
	String outType
	
	/**
	 * Enables generating test data from output of specified methods
	 * 
	 * @param obj - object being intercepted
	 * @param saveLocation - location to place generated test data.
	 * @param ignore - flag to ignore trait
	 * @param methods - list of object methods to store output.
	 * @param closure
	 */
	void provideTestData(def obj, String saveLocation, String methodPrefix = null, boolean ignore = false, List<String> methods= null,  String type = 'json', Closure closure) {
		doRun = true
		this.methods = methods
		this.methodPrefix = methodPrefix
		this.ignore = ignore
		this.saveLocation = saveLocation
		this.outType = type
		def proxy = ProxyMetaClass.getInstance(obj.class)
		proxy.interceptor = this
//		proxy.use closure
		def cMetaClass = obj.metaClass
		obj.metaClass =  proxy
		closure.call()
		obj.metaClass = cMetaClass
		proxy.interceptor = null
			
	}
	
	@Override
	public Object beforeInvoke(Object object, String methodName, Object[] arguments) {
		
		doRun = true
	}

	@Override
	public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
		if (!ignore && (!methods || methods.contains(methodName))) {
			File saveDir = new File(saveLocation)
			String outMethodName = methodName
			if (methodPrefix != null) outMethodName = "${methodPrefix}_${methodName}"
			if (outType == 'json') {
				File jFile = new File(saveDir, "${outMethodName}.json")
				try {
					String json = new JsonBuilder(result).toPrettyString()
					def os = jFile.newDataOutputStream()
					os << json
					os.close()
				} catch (e) {}
			} else if (outType == 'xml') {
				File jFile = new File(saveDir, "${outMethodName}.xml")
				try {
					String xml = new XmlUtil().serialize(result)
					def os = jFile.newDataOutputStream()
					os << xml
					os.close()
				} catch (e) {}

			}
		}
		return result
	}

	@Override
	public boolean doInvoke() {
		
		return doRun
	}

}
