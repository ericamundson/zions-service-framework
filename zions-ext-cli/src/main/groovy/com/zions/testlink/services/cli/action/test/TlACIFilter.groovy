package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlACIFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name == 'RPI Test Plan: Test Cycle 4' ||
			name == 'RPI Test Plan: Test Cycle 5' ||
			name == 'SWIFT 7.0 Upgrade'	||
			name == 'test plan test 1-2-14' ||
			name == 'TG Wires Regression-2012' ||
			name == 'TIB/MTS MQ Distributed Change' ||
			name == 'UAT: Data Conversion' ||
			name == 'UAT: Validation Test Plan' ||
			name == 'Wires IQ/MQ Phase 1' ||
			name == 'Wires IQ/MQ Phase 2' ||
			name == 'Wires IQ/MQ Regression 1' ||
			name == 'Wires IQ/MQ Regression 2'
			)
		}
	}

}
