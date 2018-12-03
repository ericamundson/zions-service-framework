package com.zions.clm.services.cli.action.work

import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter

/**
 * Filter work items for Mobile Banking.
 * 
 * <p><b>Design:</b></p>
 * <img src="MbFilter.png"/>
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
	Date startDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", '2018-10-02T00:00:01.000-0700')
	public def filter(def workItems) {
		List<String> excluded = ["Track Build Item", "Retrospective", "Adhoc Request", "Adoption Item","Enhancement Request"]
		return workItems.workItem.findAll { wi ->
			String type = "${wi.type.name.text()}"
			//2017-01-09T11:59:02.780-0700
			Date mDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "${wi.modified.text()}")
			long cT = startDate.time
			long wT = mDate.time
			("${wi.state.group.text()}" != 'closed' || wT > cT) && "${wi.target.archived.text()}" == 'false' && !excluded.contains(type)
		}
	}

}
