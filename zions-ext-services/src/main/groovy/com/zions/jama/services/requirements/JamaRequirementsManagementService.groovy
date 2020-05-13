package com.zions.jama.services.requirements

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import org.apache.commons.io.IOUtils



/**
 * Handles queries into Jama.
 * 
 */

@Slf4j
@Component
class JamaRequirementsManagementService {
	@Autowired
	@Value('${jama.url}')
	String jamaUrl
		
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired
	IGenericRestClient jamaGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
		

	
	JamaRequirementsManagementService() {
	}
		
	//single giant query but should cache ok
	def query() {
		String uri = this.jamaGenericRestClient.jamaUrl + "/rest/latest/projects/26316/itemtypes"
		def results = jamaGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/json'] );
		def i = 1
	}
	
}