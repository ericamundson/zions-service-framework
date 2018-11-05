package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClmTestItemAttributeManagerService {
	
	@Autowired
	TestMappingManagerService testMappingManagerService
	
	public ClmTestItemAttributeManagerService() {
		
	}
	
	def generateItemData(def qmItemData) {
		String type = getTestMap(qmItemData)
		def outItem = []
	}

	String getTestMap(qmItemData) {
		String type = qmItemData.name()
		def map = testMappingManagerService.mappingData.each { amap -> 
			"${amap.source}" == "${type}"
		}
		return map
	}
}
