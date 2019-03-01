package com.zions.vsts.services.policy.rest;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod
import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper

import com.zions.vsts.services.policy.PolicyManagementService;

/**
 * ReST Controller for TFS Policy Management service. 
 * @author James McNabb
 * 
 */
@RestController
@Slf4j
public class PolicyEndPoint {

    @Autowired
    private PolicyManagementService policyManagementService;

    /**
     * Create branch policies for new Git branch when created. 
     *  
     * @param ?? - 
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity newBranchCreated(@RequestBody String json) {
		//log.debug("In PolicyEndPoint - json:\n"+json)
		try {
			JsonSlurper slurper = new JsonSlurper()
			def eventData = slurper.parseText(json)
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
