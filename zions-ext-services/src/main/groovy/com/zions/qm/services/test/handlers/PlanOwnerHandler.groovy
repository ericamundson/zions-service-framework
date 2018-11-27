package com.zions.qm.services.test.handlers

import com.zions.common.services.work.handler.IFieldHandler
import com.zions.qm.services.test.ClmTestManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PlanOwnerHandler implements IFieldHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public PlanOwnerHandler() {}

	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		
		def aValue = [:]
		String ownerUrl = "${itemData.owner.@'ns7:resource'}"
		if (ownerUrl == null || ownerUrl.length() == 0) return null
		def ownerInfo = clmTestManagementService.getTestItem(ownerUrl)
		String email = "${ownerInfo.emailAddress.text()}".toLowerCase()
		def identity = memberMap[email]
		if (identity == null) return null

		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: identity]
		if (wiCache != null) {
			String type = "${itemMap.target}"
			if (type != 'Test Case') {
				def cVal = wiCache."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
			} else {
				String cVal = wiCache.fields."${fieldMap.target}"
				if ("${cVal}" == "${retVal.value}") {
					return null
				}
	
			}
		}
		return retVal;
	}
	

}
