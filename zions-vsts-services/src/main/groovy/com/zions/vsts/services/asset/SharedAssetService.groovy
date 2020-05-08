package com.zions.vsts.services.asset


import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovy.time.TimeCategory
import groovy.time.TimeDuration

/**
 * Retrieves and caches shared resources from the Azure DevOps extension data store.
 * <ul>
 * <li>
 * Can retrieve and cache any number of resources.
 * </li>
 * <li>
 * Cache expires after 1 day.
 * </li>
 * </ul>
 * @author z097331
 *
 */

@Component
@Slf4j
class SharedAssetService {
	def assetMap = [:]
	Date lastTimestamp
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	public SharedAssetService() {
	}
	
	def getAsset(String collection, String controlUID)	{
		// If more than 1 day since retrieving asset from ADO, then clear cache
		def curTimestamp = new Date()
		long elapsedDays = 1
		if (lastTimestamp != null ) {
			elapsedDays = (curTimestamp.getTime()- lastTimestamp.getTime()) / 86400000
		}
		if (elapsedDays >= 1) assetMap = [:]
		if (!assetMap.containsKey(controlUID)) {
			if (lastTimestamp == null) {
				log.info("Retrieving colorMap from ADO.")
			} else {
				log.info("Retrieving colorMap from ADO.  Last refresh more than $elapsedDays days ago.")
			}
			def result = genericRestClient.get(
					uri: "https://extmgmt.dev.azure.com/${collection}/_apis/ExtensionManagement/InstalledExtensions/$controlUID",
					contentType: ContentType.JSON,
					query: ['api-version': '4.0-preview']
					)
			assetMap.put(controlUID, result.value)
		}
		lastTimestamp = curTimestamp
		return assetMap[controlUID]
	}
	
	
}
