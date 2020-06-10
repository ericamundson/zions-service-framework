package com.zions.vsts.services.policy.rest;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod
import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper

import com.zions.vsts.services.policy.PolicyManagementService;
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait

/**
 * ReST Controller for TFS Policy Management service. 
 * @author James McNabb
 * 
 */
@Component
@Slf4j
public class PolicyEndPoint implements MessageReceiverTrait {

    @Autowired
    private PolicyManagementService policyManagementService;

	@Autowired
	public PolicyEndPoint() {
		//init(websocketUrl, null, null)
	}

    /**
     * Create branch policies for new Git branch when created. 
     *  
     * @return
     */
	public Object processADOData(Object adoData) {
		//log.debug("In PolicyEndPoint - adoData:\n"+adoData)
		try {
			//JsonSlurper slurper = new JsonSlurper()
			//def eventData = slurper.parseText(adoData)
			def eventData = adoData
			def changeSet = eventData.resource;
			changeSet.refUpdates.each { update ->
				// only protect certain branch types / patterns
				String branchName = "${update.name}".toLowerCase()
				if (branchName.startsWith("refs/heads/master") || 
					branchName.startsWith("refs/heads/release") ||
					branchName.startsWith("refs/heads/feature/ifb") ||
					branchName.startsWith("refs/heads/ifb/") ||
					branchName.startsWith("refs/heads/dr/")) {
					// only when branch is new
					if ("${update.oldObjectId}" == "0000000000000000000000000000000000000000") {
						log.debug("In PolicyEndPoint - changes:\n"+changeSet)
						def collection = getCollectionName(eventData.resourceContainers);
						policyManagementService.handleNewBranch(changeSet, collection, update.name)
					}
				}
			}
		} catch (err) {
			log.error("Error:  ${err.message}")
			return new ResponseEntity<Object>("${err.message}", HttpStatus.UNPROCESSABLE_ENTITY)
		}
		return ResponseEntity.ok(HttpStatus.OK)
	}

//	@Override
//	public String topic() {
//		return 'git.push';
//	}

    private def getCollectionName(def containerData) {
		def collectionName = ""
    	try {
	    	def serverUrl = containerData.server.baseUrl
	    	def collectionUrl = containerData.collection.baseUrl
	    	collectionName = collectionUrl.substring(serverUrl.length(), collectionUrl.length()-1)
    	} catch (err) {
    		// collection name is not available for VSTS
    		log.info("PolicyEndPoint - No collection name when ADO event trapped: ${err.message}")
    	}
    	return collectionName
    }
    private def getCollectionId(def containerData) {
    	def collectionId = containerData.collection.id
    	return collectionId
    }
 }
