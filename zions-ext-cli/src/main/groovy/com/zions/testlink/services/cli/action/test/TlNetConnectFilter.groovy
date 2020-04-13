package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter
class TlNetConnectFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name != '15436 - Strong Auth upgrade - Web Client testing' ||
			name != '4.2 Patch 1'  ||
			name != '4.2 Patch 2'  ||
			name != '4.2 Patch 3'  ||
			name != '4.2 Patch 4'  ||
			name != '4.2 Patch 5.1'	||
			name != '4.2 Performance' ||
			name != '5.1 RSS'  ||
			name != '5.1 with 4.2 - Integration' || 
			name != '5.1 with 4.2 Client'  ||
			name != '5.1 with Thick - Integration'  ||
			name != '5.1 with thick client'  ||
			name != '5.1 with thin client'  ||
			name != '5.9 Receivables' ||
			name != '5.9 Small Business'  ||
			name != '5.9 Upgrade'  ||
			name != '7.x RSS Upgrade Phase 1'
			
			)
		}
	}

}
