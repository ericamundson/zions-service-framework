package com.zions.vsts.services.setowner

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder

/**
 * Assigns unassigned tasks to the parent's owner when the task is closed.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class SetOwnerMicroService implements MessageReceiverTrait {
	@Autowired
	WorkManagementService workManagementService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.types}')
	String[] wiTypes
	
	@Value('${tfs.project.includes}')
	String[] includeProjects

	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedId
	def attemptedOwner
	Closure responseHandler = { resp ->
		if (resp.status == 412) {
			// Get fresh copy of work item
			def wi = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			def owner = wi.fields.'System.AssignedTo'
			String rev = "${wi.rev}"
			if (owner == 'null' || owner == null ) { // Process if still unassigned
				if (setOwner(this.attemptedProject, this.attemptedId, rev, this.attemptedOwner)) {
					return logResult('Work item successfully assigned after 412 retry')
				}
				else {
					this.retryFailed = true
					log.error('Failed update after 412 retry')
					return 'Failed update after 412 retry'
				}
	
			}
			return
		}
	}

	@Autowired
	public SetOwnerMicroService() {		
	}

	/**
	 * Perform assignment operation
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
//		Uncomment code below to capture adoData payload for test
//		String json = new JsonBuilder(adoData).toPrettyString()
//		println(json)
		def outData = adoData
		def wiResource = adoData.resource
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		log.debug("Entering SetOwnerMicroService:: processADOData for $project, $wiType #$id")
		if (includeProjects && !includeProjects.contains(project)) return logResult('Project not included')
		String owner = "${wiResource.revision.fields.'System.AssignedTo'}"
		String status = "${wiResource.revision.fields.'System.State'}"
		if (!wiTypes.contains(wiType)) return logResult('Not a target work item type')
		if (owner && owner != 'null') return logResult('Work item already assigned')
		if (status != 'Closed' || !wiResource.fields.'System.State') return logResult('Work item not being closed')
		String rev = "${wiResource.revision.rev}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		def revisedBy = wiResource.revisedBy
		String revisedUsername = "${revisedBy.uniqueName}"
		if (revisedUsername.toLowerCase().indexOf('svc-') == -1) { // This was not closed by a service account
			// Set ownership to user that closed this work item
			log.info("Updating owner of $wiType #$id")
			if (setOwner(project, id, rev, revisedBy, responseHandler)) {
				return logResult('Work item successfully assigned to modifier')
			}
			else if (this.retryFailed) {
				log.error('Error updating System.AssigedTo')
				return 'Error assigning to modifier'
			}
		}
		else if (parentId && parentId != 'null') {
			// Set ownership to parent's owner
			def parentWI = workManagementService.getWorkItem(collection, project, parentId)
			if (!parentWI) {
				log.error("Error retrieving work item $parentId")
				return 'Error Retrieving Parent'
			}
//			Uncomment code below to capture parent playload for test
//			String json = new JsonBuilder(parentWI).toPrettyString()
//			println(json)
			def parentOwner = parentWI.fields.'System.AssignedTo'
			if (parentOwner == 'null' || parentOwner == null) return logResult('Parent is unassigned')
			
			log.info("Updating owner of $wiType #$id")
			if (setOwner(project, id, rev, parentOwner, responseHandler)) {
				return logResult('Work item successfully assigned to parent')
			}
			else if (this.retryFailed) {
				log.error('Error updating System.AssigedTo')
				return 'Error assigning to parent owner'
			}
		}
		else {
			return logResult('No parent')
		}
	}

	private def setOwner(def project, def id, String rev, def owner, Closure respHandler = null) {
		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		def e = [op: 'add', path: '/fields/System.AssignedTo', value: owner.uniqueName]
		data.add(e)
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedId = id
		this.attemptedOwner = owner
		return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
	}
	private def logResult(def msg) {
		log.debug("Result: $msg")
		return msg
	}

}

