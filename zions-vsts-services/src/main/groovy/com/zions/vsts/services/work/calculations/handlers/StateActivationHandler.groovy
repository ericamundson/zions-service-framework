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
public class StateActivationHandler extends BaseCalcHandler {
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	public String execute(String targetField, def fields) {
		def state = fields['State']
		if (state == 'Active') return state
		
		def id = fields['ID']
		if (id instanceof Integer)
			id = id.toString()
		def areaPath = fields['Area Path']
		// throw error if no Area Path
		if (!areaPath)
			throw new Exception("Area Path is required by handler: ${this.getClass().getName()}")
		def project = getProjectFromAreaPath(areaPath)
		//Get work item children
		def wiChildren = workManagementService.getChildren(collection, project, id)
		boolean childOpen = false
		if (wiChildren) {
			wiChildren.each { child ->
				if (child.fields['System.State'] != 'Closed' ) childOpen = true
			}
		}
		if (childOpen)
			return 'Active'
		else
			return state
	}
}
