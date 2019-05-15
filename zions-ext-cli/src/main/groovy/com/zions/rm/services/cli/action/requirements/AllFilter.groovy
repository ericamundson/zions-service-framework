package com.zions.rm.services.cli.action.requirements

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class AllFilter implements IFilter {

	public def filter(def items) {
		return items.each { req ->
			true
		}
	}

}
