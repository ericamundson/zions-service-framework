package com.zions.qm.services.test.handlers

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.extension.IExtensionData
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import java.nio.charset.Charset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('QmCustomAttributesHandler')
@Slf4j
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
	
	@Autowired
	ICacheManagementService cacheManagementService

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
			String key = "${type}_Custom.CustomAttributes_${tfsProjectName}"
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
		def wiCache = data.cacheWI
		def prevWI = data.prevWI
		def catMap = [:]
		itemData.category.each { cat -> 
			String name = "${cat.@term}".replace(' ', '_')
			String avalue = "${cat.@value}"
			avalue = avalue.replaceAll("[^\\x00-\\x7F]", "");
			
			if (!catMap["${name}"]) {
				catMap["${name}"] = []
			}
			catMap["${name}"].push(avalue)
		}
		String eid = getKey(wiCache)
		catMap.each { key, vals -> 
			def desc = descMap[key]
			if (desc && canChange(prevWI, wiCache, desc.fieldName, eid)) {
				def catData = [name: key, value: vals.join(';')]
				outData.push(catData)
			}
		}
		itemData.customAttributes.customAttribute.each { att ->
			String name = "${att.identifier.text()}"
			def desc = descMap[name]
			if (desc && canChange(prevWI, wiCache, desc.fieldName, eid)) {
				String avalue = "${att.value.text()}"
				avalue = avalue.replaceAll("[^\\x00-\\x7F]", "");
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
		def prevWI = data.prevWI
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
					//String fieldName = 
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
	
	boolean canChange(prevWI, cacheWI, String tName, String key) {
		if (!cacheWI) return true
		boolean flag = true
		//String tName = "${field.target}"
		def fModified = cacheManagementService.getFromCache("${key}-${tName}", 'changedField')
		if (fModified) {
			def cVal = cacheWI.fields."${tName}"
			String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
			return false
		}
		if (!prevWI) return true
		def cVal = cacheWI.fields."${tName}"
		String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
		def pVal = prevWI.fields."${tName}"
		flag = "${pVal}" == "${cVal}"
		if (!flag) {
			log.info("ADO field change cached:  key: ${key}-${tName}, date: ${changedDate}, value: ${cVal}.")
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
		}
		return flag
	}

	private String getKey(def wi) {
		String eId = "${wi.fields['Custom.ExternalID']}"
		String wiType = "${wi.fields.'System.WorkItemType'}"
		String key = "${eId.substring(4)}-${wiType}"
		if (wiType != 'Test Case') {
			key = "${key} WI"
		}
		return key
	}

}
