package com.zions.qm.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class AllFilter implements IFilter {

	public def filter(def items) {
		return items.entry.findAll { wi ->
			true
		}
	}

}
