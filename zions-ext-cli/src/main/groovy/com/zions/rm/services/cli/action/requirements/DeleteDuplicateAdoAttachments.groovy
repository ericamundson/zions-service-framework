package com.zions.rm.services.cli.action.requirements

import java.util.Map

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
import groovyx.net.http.ContentType


/**
 * Provides command line interaction for cleaning out duplicate attachments.
 * Only duplicates that are not referenced in Description are deleted.
 */
@Component
@Slf4j
class DeleteDuplicateAdoAttachments implements CliAction {
	/**
	 * Main interface to handle requests to VSTS collection.
	 */
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	ClmRequirementsFileManagementService rmFileManagementService
	
	public DeleteDuplicateAdoAttachments() {
	}

	public def execute(ApplicationArguments data) {

		def includes = [:]
		try {
			String includeList = data.getOptionValues('include.update')[0]
			def includeItems = includeList.split(',')
			includeItems.each { item ->
				includes[item] = item
			}
		} catch (e) {}
		String queryID = data.getOptionValues('tfs.queryID')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		
		def changes
		int changeCount = 0 //kinda wanna put this in ChangeListManager but w/e
		ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/wiql/${queryID}",
			query: ['api-version': '5.1'],
			withHeader: true
			)
		// Get work item description
		result.data.workItems.each { wi ->
			def attachmentMap = [:]
			def createdDates = [:]
			def relationsIndex = [:]
			String id = "${wi['id']}"
			String url = "${wi['url']}"
			def wiResult = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/workitems?ids=${id}&\$expand=all",
				query: ['api-version': '5.1'],
				withHeader: true
				)
			//Save Description and rev
			String description = "${wiResult.data.value.fields.'System.Description'}"
			Integer revNum = wiResult.data.value.rev[0]
			String cid = "${wiResult.data.value.fields.'Custom.ExternalID'[0]}".replace('DNG-','')
			//Save attachments
			def attachments = wiResult.data.value.relations
			def ndx = 0
			attachments[0].each { attachment ->
				if (attachment.rel == 'AttachedFile') {
					String ref = "${attachment.url}"				
					String name = "${attachment.attributes.name}"
					String created = "${attachment.attributes.resourceCreatedDate}"
					attachmentMap.put(ref,name)
					createdDates.put(ref,created)
					relationsIndex.put(ref,ndx)
				}
				ndx++
			}
			// Group attachments by name so we can easily find duplicates
			attachmentMap = attachmentMap.groupBy{it.value}
			// Get json changes for REST call
			changes = getAttachmentDeletionChanges(id, revNum, description, attachmentMap, createdDates, relationsIndex)
			if (changes != null) {
				clManager.add("${cid}", changes)
				//log.debug("adding changes for requirement ${id}")
			}
		}
		log.debug("have ${clManager.size()} changes, about to flush clmanager")
		changeCount+= clManager.size()
		clManager.flush();
		log.debug("finished flushing clmanager")
		return
	}
	
	private def getAttachmentDeletionChanges(def id, def revNum, def description, def attachmentMap, def createdDates, def relationsIndex) {
		def deleteCount = 0
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${id}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def rev = [ op: 'test', path: '/rev', value: revNum]
		wiData.body.add(rev)
		attachmentMap.each {
			if (it.value.size() > 1) {  // more than one attachment with same name
				// Check if any are embedded in Description
				def embedCount = 0
	            it.value.each { if (description.indexOf(it.key) > -1) embedCount++ }
				// If none are embedded, keep latest one and delete all others
				if (embedCount == 0) {
					def refMostRecent 
					def refDate = "2000-01-01T20:38:20.717Z"
					it.value.each { 
						if (createdDates[it.key] > refDate) {
							refMostRecent = it.key
							refDate = createdDates[it.key]
						}
					}
					it.value.each { 
						if (it.key != refMostRecent) {
							// Delete it.key
							deleteCount++
							wiData.body.add(getAttachmentChange(id, it, relationsIndex))
						}
					}
				}
				// Else, delete all attachments except those that are embedded
				else {
					it.value.each {
						if (description.indexOf(it.key) == -1) {
							// Delete it.key
							deleteCount++
							wiData.body.add(getAttachmentChange(id, it, relationsIndex))
						}
					}
				}
			}
        }
		if (deleteCount == 0) {
			return null
		}
		else {
			return wiData
		}
	}
	private def getAttachmentChange(def id, def it, def relationsIndex) {
		log.debug("Deleting attachment for $id: ${it.value}-${it.key}")
		def change = [op: 'remove', path: "/relations/${relationsIndex[it.key]}"]
		return change
	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.user', 'tfs.queryID']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
