package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlCSFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name == 'Sprint 4/5' ||
			name == 'TEST3 Regression' 
			
			)
		}
	}

}
