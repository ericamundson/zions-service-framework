package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

/**
 * Work item filter for Online Banking team.
 * 
 * <p><b>Design:</b></p>
 * <img src="ObFilter.svg"/>
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
		List<String> excluded = ["Track Build Item", "Retrospective",  "Adoption Item", "Infrastrucure Request"]
		return workItems.workItem.findAll { wi ->
			String type = "${wi.type.name.text()}"
			String target = "${wi.target.name.text()}"
			String wiState = "${wi.state.group.text()}"
			String archivedTarget = "${wi.target.archived.text()}"
			boolean openParentState = false
			String parentState = "${wi.parent.state.group.text()}"
			String parentType = "${wi.parent.type.name.text()}"
			String parentArchivedTarget = "${wi.parent.target.archived.text()}"
			if (parentState.length() > 0 && parentState != 'closed' && parentArchivedTarget == 'false' && !excluded.contains(parentType)) {
				openParentState = true
			}
			boolean openRelatedState = false
			String relatedState = "${wi.related.state.group.text()}"
			String relatedType = "${wi.related.type.name.text()}"
			String relatedArchivedTarget = "${wi.related.target.archived.text()}"
			if (relatedState.length() > 0 && relatedState != 'closed' && !excluded.contains(relatedType)) {
				openRelatedState = true
			}
			boolean val = openParentState || (wiState != 'closed'  && archivedTarget == 'false' && !excluded.contains(type))
			return val
		}
	}

}
