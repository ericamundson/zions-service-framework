package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.common.services.work.handler.IFieldHandler

@Component
public class ClmTestItemManagementService {
	
	@Autowired
	private Map<String, IFieldHandler> fieldMap;

	@Autowired
	TestMappingManagerService testMappingManagerService
	
	public ClmTestItemManagementService() {
		
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
