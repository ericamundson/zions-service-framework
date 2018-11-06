package com.zions.clm.services.ccm.workitem.handler

import com.ibm.team.links.client.ILinkManager
import com.ibm.team.links.common.ILink
import com.ibm.team.links.common.ILinkCollection
import com.ibm.team.links.common.ILinkQueryPage
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.factory.IReferenceFactory
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.common.IAuditableHandle
import com.ibm.team.scm.common.IChangeSet
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.common.services.work.handler.IFieldHandler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RemainingWorkHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	def mappingData = null;

	public ImplementedDateHandler() {}


	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def wiMap = data.wiMap
		String sValDuration = workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), 'duration')
		String sValTimeSpent  = workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), 'timeSpent')
		double duration = 0;
		try {
			duration = Double.parseDouble(sValDuration)
		} catch (err) {}
		double timeSpent = 0;
		try {
			timeSpent = Double.parseDouble(sValTimeSpent);
		} catch (err) {}
		double retNumber = duration - timeSpent;
		if (retNumber < 0) {
			retNumber = 0.0;
		}
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: "${retNumber}"]
		String state = determineState(wi, wiMap)
		if ("${state}" == 'Closed') {
			retVal = [op:'add', path:"/fields/${fieldMap.target}", value: '']
		}
		if (wiCache != null) {
			def cVal = wiCache.fields."${fieldMap.target}"
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	
	def getStateFieldMap(wiMap) {
		
		def map = wiMap.fieldMaps.find { fieldMap ->
			"${fieldMap.source}" == 'internalState'
		}
		return map
	}
	
	String determineState(IWorkItem wi, def wiMap) {
		def fieldMap = getStateFieldMap(wiMap)
		String val = workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), 'internalState')
		fieldMap.valueMap.each { aval ->
			if ("${val}" == "${aval.source}") {
				val = "${aval.target}"
				return
			}
		}
		return val
	}

}
