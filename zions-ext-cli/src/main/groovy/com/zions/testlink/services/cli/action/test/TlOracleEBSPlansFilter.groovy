package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlOracleEBSPlansFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			name == 'Oracle Upgrade TEBUS 3 Unit Testing 1'
		}
	}

}
