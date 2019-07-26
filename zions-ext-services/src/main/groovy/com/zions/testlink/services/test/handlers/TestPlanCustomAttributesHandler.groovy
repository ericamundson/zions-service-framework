package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.CustomField
import br.eti.kinoshita.testlinkjavaapi.model.ResponseDetails
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import com.zions.common.services.extension.IExtensionData
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.testlink.services.test.TestLinkClient
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('TlTestPlanCustomAttributesHandler')
class TestPlanCustomAttributesHandler extends TlBaseAttributeHandler {
	

	@Autowired(required=false)
	IExtensionData extensionData
	
	@Autowired(required=false)
	TestLinkClient testlinkClient

	@Value('${tfs.project}')
	String projectName

	def descMap = [:]
	
	public TestPlanCustomAttributesHandler() {
	}

	public String getFieldName() {
		return 'none'
	}
	
	def initDescMap() {
		if (descMap.size() > 0) return descMap
		String key = "Test Plan_Custom.CustomAttributes_${projectName}"
		def descs = extensionData.getExtensionData(key)
		descs.descriptors.each { desc -> 
			descMap[desc.name] = desc
		}
	}

	public def formatValue(def value, def data) {
		def outData = [];
		TestCase itemData = data.itemData
		descMap.each { name, def desc ->
			CustomField cf = null
			try {
				cf = testlinkClient.getTestCaseCustomFieldDesignValue(itemData.id, null, null, itemData.testProjectId, name, ResponseDetails.FULL )
			} catch (e) {}
			if (cf) {
				def val = [name: cf.name, value: cf.value ]
				outData.push(val)
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
		
		String name = getFieldName()
		def aValue = null;
		if (itemData.hasProperty("${name}")) {
			aValue = itemData."${name}"
		}
		//aValue = formatValue(aValue, data)
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
//		if (aValue != null) {
//			def oValues = aValue.'values'.findAll { aVal ->
//				def desc = descMap[aVal.name]
//				!desc || !desc.fieldName
//				
//			}
//			if (oValues && oValues.size()>0) {
//				def nValue = [values: oValues]
//				String mapVal = new JsonBuilder(nValue).toString()
//				retVal.push([op:'add', path:"/fields/${fieldMap.target}", value: mapVal])
//			}
//		}
//		aValue.'values'.each { val ->
//			if (descMap[val.name]) {
//				String fieldName = descMap[val.name].fieldName
//				if (fieldName) {
//					def aField = [op:'add', path:"/fields/${fieldName}", value: val.value]
//					retVal.push(aField)
////					def aField = getExisting(retVal, fieldName)
////					if (!aField) {
////						aField = [op:'add', path:"/fields/${fieldName}", value: val.value]
////						retVal.push(aField)
////					} else {
////						String oval = "${aField.value};${val.value}"
////						aField.'value' = oval
////					}
//				}
//			}
//		}
		def eId = "TL-${itemData.id} WI"
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
