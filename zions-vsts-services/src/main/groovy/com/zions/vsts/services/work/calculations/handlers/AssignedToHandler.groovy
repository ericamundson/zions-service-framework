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
public class AssignedToHandler extends BaseCalcHandler {
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	public String execute(String targetField, def fields) {
		def assignedTo = fields['Assigned To']
		if (assignedTo != null && assignedTo != '') {
			return assignedTo
		}
		
		def id = fields['ID']
		if (id instanceof Integer)
			id = id.toString()
		def areaPath = fields['Area Path']
		// throw error if no Area Path
		if (!areaPath)
			throw new Exception("Area Path is required by handler: ${this.getClass().getName()}")
		def project = getProjectFromAreaPath(areaPath)
		//Get work item
		def wi = workManagementService.getWorkItem(collection, project, id)
		def wiParent = workManagementService.getParent(collection, project, wi)
		if (wiParent)
			return wiParent.fields['System.AssignedTo'].uniqueName
		else
			return null
	}
}
