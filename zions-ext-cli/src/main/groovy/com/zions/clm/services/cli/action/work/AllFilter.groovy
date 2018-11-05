package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class AllFilter implements IFilter {

	public def filter(def workItems) {
		return workItems.workItem.findAll { wi ->
			true
		}
	}

}
