package com.zions.vsts.services.work.calculations.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.work.WorkManagementService

import groovy.util.logging.Slf4j

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

/**
 * Base class for producing work item field entries.
 * 
 * @author z091182
 * 
 * @startuml
 * 
 * abstract class RmBaseAttributeHandler {
 * + def execute(def data)
 * + <<abstract>> String getFieldName()
 * + <<abstract>> def formatValue(def val, def itemData)
 * }
 * 
 * IFieldHandler <|.. RmBaseAttributeHandler
 * 
 * @enduml
 * 
 *
 */
@Component
@Slf4j
public class ReopenCountHandler extends BaseCalcHandler {
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.sourcestates:}')
	String sourceStates
	
	@Value('${tfs.deststates:}')
	String destStates

	public String execute(String targetField, def fields) {
		def id = fields['ID']
		if (id instanceof Integer)
			id = id.toString()
		def areaPath = fields['Area Path']
		// throw error if no Area Path
		if (!areaPath)
			throw new Exception("Area Path is required by handler: ${this.getClass().getName()}")
		if (!sourceStates || !destStates)
			throw new Exception("Both tfs.sourcestates and tfs.deststates parameters are required by handler: ${this.getClass().getName()}")
		def project = getProjectFromAreaPath(areaPath)
		//Get work item
		def history = workManagementService.getWorkItemUpdates(collection, project, id)
		int reopenCount = 0
		if (history) {
			def changeList = history.value
			changeList.each { change ->
				if (change.fields) {
					def stateChange = change.fields['System.State']
					if (stateChange && stateChange.oldValue) {
						if (sourceStates.contains(stateChange.oldValue) && destStates.contains(stateChange.newValue)) 
							reopenCount++
					}
				}
			}
		}
		return reopenCount
	}
}
