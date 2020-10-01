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
public class ColorCalcHandler implements IFieldHandler {
	@Autowired
	SharedAssetService sharedAssetService
	
	@Value('${tfs.collection:}')
	String collection

	@Value('${tfs.colorMapUID:}')
	String colorMapUID

	public Object execute(Object data) {
		def priority = data.fields['Priority']
		def severity = data.fields['Severity']
		def colorMap = sharedAssetService.getAsset(collection, colorMapUID)
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return colorElement.Color
	}
	

}
