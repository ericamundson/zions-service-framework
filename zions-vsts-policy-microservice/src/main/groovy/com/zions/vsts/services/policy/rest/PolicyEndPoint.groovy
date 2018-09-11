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
		//log.info("In PolicyEndPoint - json:\n"+json)
		try {
			JsonSlurper slurper = new JsonSlurper()
			def eventData = slurper.parseText(json)
			def changeSet = eventData.resource
			def collection = getCollectionName(eventData.resourceContainers)
			changeSet.refUpdates.each { update ->
				if ("${update.name}".startsWith("refs/heads/master") || 
					"${update.name}".startsWith("refs/heads/release") ||
					"${update.name}".toLowerCase().startsWith("refs/heads/feature/ifb")) {
					if ("${update.oldObjectId}" == "0000000000000000000000000000000000000000") {
						policyManagementService.handleNewBranch(changeSet, collection, update.name)
					}
				}
			}
		} catch (err) {
			log.error("Error:  ${err.message}")
			return ResponseEntity.unprocessableEntity()
		}
		return ResponseEntity.ok(HttpStatus.OK)
	}

    private def getCollectionName(def containerData) {
    	try {
	    	def serverUrl = containerData.server.baseUrl
	    	def baseUrl = containerData.server.baseUrl
	    	def collectionUrl = containerData.collection.baseUrl
	    	def collectionName = collectionUrl.substring(serverUrl.length(), collectionUrl.length()-1)
    	} catch (Exception) {
    		// collection name is not available for VSTS
    		return ""
    	}
    	return collectionName
    }
    private def getCollectionId(def containerData) {
    	def collectionId = containerData.collection.id
    	return collectionId
    }
 }
