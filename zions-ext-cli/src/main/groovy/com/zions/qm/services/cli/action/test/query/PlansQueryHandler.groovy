package com.zions.qm.services.cli.action.test.query

import org.springframework.stereotype.Component

@Component
class PlansQueryHandler extends BaseQueryHandler {
	public Object nextPage() {
		def nextLink = currentItems.'**'.find { node ->
	
			node.name() == 'link' && node.@rel == 'next'
		}
		if (nextLink == null) {
			cacheManagementService.deleteByType('wiPrevious')
			return null
		}
		this.page++
		String pageId = "${page}"
		//new CacheInterceptor() {}.provideCaching(clmTestManagementService, pageId, currentTimestamp, TestPlanQueryData) {
			currentItems = clmTestManagementService.nextPage(nextLink.@href)
		//}
		return currentItems
	}

}
