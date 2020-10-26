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
public class PropagatedValueHandler extends BaseCalcHandler {
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	public String execute(String targetField, def fields) {
		def id = fields['ID']
		if (id instanceof Integer)
			id = id.toString()
		def project = fields['Team Project']
		// throw error if no Team Project
		if (!project)
			throw new Exception("Team Project is required by handler: ${this.getClass().getName()}")
		//Get work item
		def wi = workManagementService.getWorkItem(collection, project, id)
		def wiParent = workManagementService.getParent(collection, project, wi)
		if (wiParent)
			return wiParent.fields[targetField]
		else
			return null
	}
}
