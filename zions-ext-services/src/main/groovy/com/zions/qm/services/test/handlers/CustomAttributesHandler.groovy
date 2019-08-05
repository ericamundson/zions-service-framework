package com.zions.qm.services.test.handlers

import com.zions.common.services.extension.IExtensionData
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService

import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('QmCustomAttributesHandler')
class CustomAttributesHandler extends QmBaseAttributeHandler {
	@Autowired(required=false)
	IExtensionData extensionData

	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Value('${clm.projectArea:}')
	String project

	@Autowired
	TestMappingManagementService testMappingManagementService
	
	@Value('${tfs.project}')
	String tfsProjectName


	def descMap = [:]
	
	public CustomAttributesHandler() {
	}

	public String getQmFieldName() {
		return 'none'
	}
	
	def initDescMap() {
		if (descMap.size() > 0) return descMap
		String[] types = ['Test Case', 'Test Suite', 'Test Plan']
		types.each { String type -> 
			String key = "Test Case_Custom.CustomAttributes_${tfsProjectName}"
			def descs = extensionData.getExtensionData(key)
			if (descs) {
				descs.descriptors.each { desc -> 
					descMap[desc.name] = desc
				}
			}
		}
		//descMap = clmTestManagementService.getDescriptorMap()
	}

	public def formatValue(def value, def data) {
		def outData = [];
		def itemData = data.itemData
		def catMap = [:]
		itemData.category.each { cat -> 
			String name = "${cat.@term}".replace(' ', '_')
			String avalue = "${cat.@value}"
			if (!catMap["${name}"]) {
				catMap["${name}"] = []
			}
			catMap["${name}"].push(avalue)
		}
		catMap.each { key, vals -> 
			def desc = descMap[key]
			if (desc) {
				def catData = [name: key, value: vals.join(';')]
				outData.push(catData)
			}
		}
		itemData.customAttributes.customAttribute.each { att ->
			String name = "${att.identifier.text()}"
			def desc = descMap[name]
			if (desc) {
				String avalue = "${att.value.text()}"
				def caData = [name: name, value: avalue]
				outData.push(caData)
			}
		}
		def out = [values:outData]
		return out;
	}

	public Object execute(Object data) {
		def itemData = data.itemData
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def itemMap = data.itemMap
		initDescMap()
		
		String name = getQmFieldName()
		def aValue = itemData."${name}".text()
		aValue = formatValue(aValue, data)
//		if (aValue == null) {
//			return null
//		} else {
//			if (aValue instanceof String) {
//				String val = "${aValue}"
//				if (fieldMap.defaultValue != null) {
//					val = "${fieldMap.defaultValue}"
//				}
//				if (fieldMap.values.size() > 0) {
//	
//					fieldMap.values.each { aval ->
//						if ("${aValue}" == "${aval.source}") {
//							val = "${aval.target}"
//							return
//						}
//					}
//				}
//				aValue = val
//			}
//		}
		def retVal = []
		if (aValue != null) {
			def oValues = aValue.'values'.findAll { aVal ->
				def desc = descMap[aVal.name]
				!desc || !desc.fieldName
				
			}
			if (oValues && oValues.size()>0) {
				def nValue = [values: oValues]
				String mapVal = new JsonBuilder(nValue).toString()
				retVal.push([op:'add', path:"/fields/${fieldMap.target}", value: mapVal])
			}
		}
		aValue.'values'.each { val ->
			if (descMap[val.name]) {
				String fieldName = descMap[val.name].fieldName
				if (fieldName) {
					def aField = getExisting(retVal, fieldName)
					if (!aField) {
						aField = [op:'add', path:"/fields/${fieldName}", value: val.value]
						retVal.push(aField)
					} else {
						String oval = "${aField.value};${val.value}"
						aField.'value' = oval
					}
				}
			}
		}
		def eId = "RQM-${itemData.webId.text()}"
		def aField = [op:'add', path:"/fields/Custom.ExternalID", value: eId]
		retVal.push(aField)

		if (cacheCheck && wiCache != null) {
			boolean changed = false
			retVal.each { rVal ->
				String fieldName = rVal.path
				fieldName = fieldName.substring(fieldName.lastIndexOf('/')+1)
				String cVal = wiCache.fields."${fieldName}"
				if (!cVal || "${cVal}" != "${rVal.value}") {
					changed = true
				}
			}
			if (!changed) {
				return null;
			}
		}
		return retVal;
	}
	
	def getExisting(retVal, fieldName) {
		String path = "/field/${fieldName}"
		def exist = retVal.find { ele ->
			String apath = "${ele.path}"
			apath == path
		}
		return exist
	}
	


}
