package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlOBFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			(name == 'Transaction Compare - NBA' ||
			name == 'Transaction Compare - NSB'  ||
			name == 'Transaction Compare - Vectra'  ||
			name == 'Transaction Compare - Zions'  ||
			name == 'Transfer regression for Training'  ||
			name == 'Transfer to a Friend'  ||
			name == 'Transfers - Account List Filtering'  ||
			name == 'Transfers - VBC Final Test'  ||
			name == 'Transfers - ZFN final test'  ||
			name == 'Transfers Validation' ||
			name == 'Validate Button Functionality - Wires'  ||
			name == 'WebSphere Application Server V8.5.5 Fix Pack 11 - Common Services 20170816' ||
			name == 'Z-Presprint - eStatements'		
			)
		}
	}

}