package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class ObFilter implements IFilter {

	public def filter(def workItems) {
		List<String> excluded = ["Track Build Item", "Retrospective", "Adhoc Request", "Adoption Item"]
		return workItems.workItem.findAll { wi ->
			String type = "${wi.type.name.text()}"
			"${wi.state.group.text()}" != 'closed' && "${wi.target.archived.text()}" == 'false' && !excluded.contains(type)
		}
	}

}
