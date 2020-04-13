package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlMagnetFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name == 'Money Transfer Review' ||
			name == 'MTS13: System/Integration' ||
			name == 'On Demand' ||
			name == 'Performance Test' ||
			name == 'Production 011808 CR Fixes' ||
			name == 'S1 Corporate Banking' ||
			name == 'SA TM Portal Upgrade' ||
			name == 'TEST S!' ||
			name == 'Treasury Management Reports' ||
			name == 'Z03 Pre Prod Fix Build 3' ||
			name == 'Z03 Preprod Issues Retest' ||
			name == 'Z03 Prod Fixes' ||
			name == 'Zions App Server Change'
			)
		}
	}

}
