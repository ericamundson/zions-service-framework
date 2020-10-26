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
public class ColorCalcHandler extends BaseCalcHandler {
	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.colorMapUID:}')
	String colorMapUID

	public String execute(String targetField, def fields) {
		def priority = fields['Priority']
		if (priority == null) // missing column
			throw new Exception("Priority is required by handler: ${this.getClass().getName()}")
		else if (priority == '') // no value
			return '' // no color
		def severity = fields['Severity']
		if (severity == null) // missing column
			throw new Exception("Severity is required by handler: ${this.getClass().getName()}")
		else if (severity == '') // no value
			return '' // no color
	
		def colorMap = sharedAssetService.getAsset(collection, colorMapUID)
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return colorElement.Color
	}
	

}
