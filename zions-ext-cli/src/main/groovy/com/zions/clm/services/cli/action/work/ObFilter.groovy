package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

/**
 * Work item filter for Online Banking team.
 * 
 * <p><b>Design:</b></p>
 * <img src="ObFilter.png"/>
 * 
 * @author z091182
 *
 * @startuml
 * class ObFilter  {
 * 	+def filter(def workItems)
 * }
 * interface IFilter [[java:com.zions.common.services.query.IFilter]] {
 * }
 * IFilter <|.. ObFilter
 * @enduml
 *
 */
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
