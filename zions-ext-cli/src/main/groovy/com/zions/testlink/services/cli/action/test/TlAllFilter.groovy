package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlAllFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			true
		}
	}

}
