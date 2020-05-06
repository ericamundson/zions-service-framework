package com.zions.vsts.services.asset


import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonSlurper

//import com.jayway.jsonpath.internal.function.text.Length


@Component
@Slf4j
class SharedAssetService {
	def colorMap
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	public SharedAssetService() {
		File colorMapFile = new File('c:/colormap.json')
		colorMap = new JsonSlurper().parseText(colorMapFile.text)
	}
	
	def getAsset(def name)	{
		return colorMap.value
	}
	
	
}
