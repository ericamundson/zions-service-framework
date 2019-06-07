package com.zions.common.services.cache

import com.zions.common.services.cacheaspect.CacheInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Concrete implementation of CacheInterceptor trait
 * 
 * @author z091182
 *
 */
@Component
class CacheInterceptorService implements CacheInterceptor {
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	/**
	 * Default constructor
	 */
	public CacheInterceptorService() {
		
	}

}
