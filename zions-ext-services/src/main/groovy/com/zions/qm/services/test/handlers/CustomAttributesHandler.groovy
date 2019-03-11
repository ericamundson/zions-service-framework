package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService

import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CustomAttributesHandler extends QmBaseAttributeHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Value('${clm.projectArea:}')
	String project

	@Autowired
	TestMappingManagementService testMappingManagementService

	def descMap = [:]
	
	public CustomAttributesHandler() {
	}

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'none'
	}
	
	def initDescMap() {
		if (descMap.size()>0) return descMap
		String[] types = ['Test Case', 'Test Suite', 'Test Plan']
		def mapping = testMappingManagementService.mappingData
		types.each { type ->
			def outDesc = []
			def excluded = []
			mapping.each { map ->
				String target = "${map.target}"
				if (target == type) {
					excluded = map.excluded
					return
				}
			}
			def cats = clmTestManagementService.getCategories(project, type)
			this.addCatDescriptors(cats, outDesc, '', excluded) 
			def ca = clmTestManagementService.getCustomAttributes(project, type)
			addCADescriptors(ca, outDesc, '', excluded)
			outDesc.each { desc -> 
				descMap[desc.name] = desc
			}
		}
		return descMap
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
			def catData = [name: key, value: vals.join(';')]
			outData.push(catData)
		}
		itemData.customAttributes.customAttribute.each { att ->
			String name = "${att.identifier.text()}"
			def desc = descMap[name]
			String avalue = "${att.value.text()}"
			def caData = [name: name, value: avalue]
			outData.push(caData)
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
	
	def addCADescriptors(ca, outDesc, tfsAreaPath, excluded) {
		def stuff = null
		ca.'soapenv:Body'.response.returnValue.values.each { item ->
			if (!item.archived) {
				if (!excluded.contains(item.identifier)) {
					def name = item.name.replaceAll("[^A-Za-z0-9 ]", "");
					name = name.replace(' ', '_')
					def fieldName = "Custom.Test${toCamelCase(name)}"
					def caItem = [name: item.identifier, displayName: item.name, fieldName: fieldName, attributeType: 'Text', areaPathsFilter: [tfsAreaPath], enumValues:[]]
					outDesc.add(caItem)
				}
			}
		}
	}
	
	def addCatDescriptors(cats, outDesc, tfsAreaPath, excluded) {
		cats.'soapenv:Body'.response.returnValue.values.each { cat ->
			if (!cat.archived) {
				String type = 'Enumeration'
				if (cat.multiSelectable) {
					type = 'Multiselect'
				}
				String displayName = "${cat.name}"
				String name = displayName.replace(' ', '_')
				if (!excluded.contains(name)) {
					String fieldName = "Custom.Test${toCamelCase(name)}"
					if (displayName == 'Automation Status') {
						displayName = "RQM ${displayName}"
					}
					def catItem = [name: name, displayName: displayName, fieldName: fieldName, attributeType: type, areaPathsFilter: [tfsAreaPath], enumValues: []]
					cat.categories.each { val ->
						if (!val.archived) {
							catItem.enumValues.add(val.name)
						}
					}
					outDesc.add(catItem)
				}
			}
		}
	}
	
	static String toCamelCase( String text) {
		text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
		return text
	}


}
