package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter
import groovy.xml.XmlUtil

/**
 * Filter work items for Mobile Banking.
 * 
 * <p><b>Design:</b></p>
 * <img src="MbFilter.svg"/>
 * 
 * @author z091182
 *
 * @startuml
 * class MbFilter  {
 * 	+def filter(def workItems)
 * }
 * interface IFilter [[java:com.zions.common.services.query.IFilter]] {
 * }
 * IFilter <|.. MbFilter
 * @enduml
 *
 *
 */
@Component
class MbFilter implements IFilter {
	//Date startDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", '2018-10-02T00:00:01.000-0700')
	public def filter(def workItems) {
		String itemXml = XmlUtil.serialize(workItems)
		
		List<String> excluded = ["Track Build Item", "Retrospective", "Adhoc Request", "Adoption Item","Enhancement Request"]
		List<String> openTypes = [new String('Story'), new String('Epic'), new String('Defect'), new String('Spike')]
		def wis = workItems.workItem.findAll { wi ->
			String type = "${wi.type.name.text()}"
			//2017-01-09T11:59:02.780-0700
			String iteration = "${wi.target.name.text()}"
			String category = "${wi.category.name.text()}"
			String wiState = "${wi.state.group.text()}"
			String wiType = "${wi.type.name.text()}"
			def relateds = [:]
			wi.related.each { related -> 
				relateds["${related.type.name.text()}"] = [state:"${related.state.group.text()}", category: "${related.category.name.text()}"]
			}
			wi.parent.each { related ->
				relateds["${related.type.name.text()}"] = [state:"${related.state.group.text()}", category: "${related.category.name.text()}"]
			}

			boolean flag = false
			
			if (category == 'Team Mario' && iteration == 'Backlog' && "${wi.target.archived.text()}" == 'false' && !excluded.contains(type)) {
				//flag = (parentState == null && wiState != 'closed') || ((wiType == 'Story' && wiType == 'Epic') && wiState != 'closed')  || ((wiType == 'Defect' || wiType == 'Task') && parentState != 'closed' && (parentType == 'Epic' || parentType == 'Story'))
				boolean openRelated = false
				def props = relateds['Story']
				if (props != null && props.state != 'closed' && props.category == 'Team Mario') {
					openRelated = true
				}
				props = relateds['Epic']
				if (props != null && props.state != 'closed' && props.category == 'Team Mario') {
					openRelated = true
				}

				flag = (wiState != 'closed' && openTypes.contains(type)) || openRelated
			}
			flag
		}
		return wis
	}

}
