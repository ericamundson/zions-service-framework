package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

/**
 * Let all work items through.
 * 
 * <p><b>Design:</b></p>
 * <img src="AllFilter.svg"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class AllFilter [[java:com.zions.clm.services.cli.action.work.AllFilter]] {
 * 	+def filter(def workItems)
 * }
 * interface IFilter [[java:com.zions.common.services.query.IFilter]] {
 * }
 * IFilter <|.. AllFilter
 * @enduml
 *
 */
@Component
class AllFilter implements IFilter {

	public def filter(def workItems) {
		List<String> excluded = ["Track Build Item", "Retrospective",  "Adoption Item", "Infrastrucure Request"]
		return workItems.findAll { wi ->
			//String type = "${wi.type.name.text()}"
			//String archivedTarget = "${wi.target.archived.text()}"
			//boolean val = !excluded.contains(type)
			return true
		}
	}

}
