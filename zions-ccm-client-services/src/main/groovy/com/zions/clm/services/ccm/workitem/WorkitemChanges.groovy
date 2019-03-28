package com.zions.clm.services.ccm.workitem

import com.zions.common.services.cacheaspect.CacheRequired
import groovy.transform.Canonical

@Canonical
class WorkitemChanges implements CacheRequired {
	def changes
}
