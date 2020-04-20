package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlARPSMSFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name == 'PDF Maintenance' ||
			name == 'QAD1 Environment Smoke Test' ||
			name == 'Real Time SMS Intraday Auto File Load' ||
			name == 'SMS ARP Sv Cd 53 upgrade Interface to Analysis' ||
			name == 'Stop Payments Testing' ||
			name == 'Teller Positive Pay Update' ||
			name == 'ZCBT: Performance/Load' ||
			name == 'ZCBT: Performance/Stress' ||
			name == 'ZCBT: System'
			)
		}
	}

}
