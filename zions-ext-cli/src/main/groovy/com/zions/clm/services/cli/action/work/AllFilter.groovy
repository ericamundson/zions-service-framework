package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

@Component
class AllFilter implements IWorkitemFilter {

	public def filter(def workItems) {
		return workItems.workItem.findAll { wi ->
			true
		}
	}

}
