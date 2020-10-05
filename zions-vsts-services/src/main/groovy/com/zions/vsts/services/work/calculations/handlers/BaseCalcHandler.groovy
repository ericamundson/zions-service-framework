package com.zions.vsts.services.work.calculations.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import com.zions.vsts.services.asset.SharedAssetService

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
abstract class BaseCalcHandler implements IFieldHandler {	
	@Value('${tfs.collection:}')
	String collection


	public Object execute(Object data) {
		return execute(data.targetField, data.fields)
	}
	
	abstract String execute(String targetField, def fields)
	
	public String getProjectFromAreaPath(String areaPath) {
		int backSlashNdx = areaPath.indexOf('\\')
		if (backSlashNdx > -1)
			return areaPath.substring(0,backSlashNdx)
		else
			return areaPath
		
	}


}
