package com.zions.testlink.services.cli.action.test

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class TlLockboxPlansFilter implements IFilter {

	public def filter(def items) {
		return items.findAll { ti ->
			String name = "${ti.name}"
			name == 'Ephesoft Formware Replacement'
		}
	}

}