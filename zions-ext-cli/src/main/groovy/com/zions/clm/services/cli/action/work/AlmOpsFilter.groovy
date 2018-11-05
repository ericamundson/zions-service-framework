package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

@Component
class AlmOpsFilter implements IFilter {

	public def filter(def workItems) {
		List<String> excluded = ["Change Request", "Spike", "Issue", "Track Build Item", "Retrospective"]
		return workItems.workItem.findAll { wi ->
			String type = "${wi.type.name.text()}"
			"${wi.state.group.text()}" != 'closed' && "${wi.target.archived.text()}" == 'false' && !excluded.contains(type)
		}
	}

}
