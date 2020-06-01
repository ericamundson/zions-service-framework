package com.zions.common.services.rest

class CollectionInterceptor extends DelegatingMetaClass  {
	

	private CollectionInterceptor(Class clz) {
		super(clz)
		initialize()
	}
	
	static public CollectionInterceptor injectIn(Class type) {
		type.metaClass = new CollectionInterceptor(type)
	}
	
	Map methodMap = ['get': '', 'post': '','put': '','patch': '','rateLimitPost': '','delete': '',]
	
	def invokeMethod(def obj, String methodName, Object[] args) {
		if (obj instanceof AGenericRestClient && methodMap.containsKey(methodName) && args[0] instanceof Map) {
			args[0] = checkBlankCollection(obj, args[0])
		}
		def val = null
      	try {
          	val = super.invokeMethod(obj, methodName, args)
      	} catch(Exception e) {
          	//println "after: $cls.$method, has thrown:$e <--"
          	throw e
      	}
		 return val
	}
	
	
	def checkBlankCollection(def obj, Map input) {
		String uri = "${input.uri}"
		String checkedUri = "${obj.tfsUrl}//"
		if (obj.tfsUrl && uri.startsWith(checkedUri) ) {
		
			uri = "${obj.tfsUrl}/${uri.substring(checkedUri.length())}"
			input.uri = uri
		}
		if (input.headers != null && input.headers.Referer != null) {
			String refUri = input.headers.Referer
			if (refUri.startsWith(checkedUri)) {
				refUri = "${obj.tfsUrl}/${refUri.substring(checkedUri.length())}"
				input.headers.Referer = refUri
			}
		}
		return input
	}

}
