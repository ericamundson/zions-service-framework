package com.zions.pipeline.services.execution.endpoint;

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
import groovy.json.JsonBuilder

import com.zions.vsts.services.code.CodeManagementService

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait

import com.zions.pipeline.services.yaml.execution.YamlExecutionService

/**
 * ReST Controller for TFS Policy Management service. 
 * @author James McNabb
 * 
 */
@Slf4j
@Component
class PipelineExecutionEndPoint implements MessageReceiverTrait {

//    @Autowired
//    private PolicyManagementService policyManagementService;
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	YamlExecutionService yamlExecutionService
	
	@Value('${pipeline.folders:.pipeline,pipeline}')
	String[] pipelineFolders


	public PipelineExecutionEndPoint() {
		//init(websocketUrl, null, null)
	}

    /**
     * Create branch policies for new Git branch when created. 
     *  
     * @return
     */
	public Object processADOData(Object adoData) {
		//log.debug("In PolicyEndPoint - adoData:\n"+adoData)
//			JsonSlurper slurper = new JsonSlurper()
//			def eventData = slurper.parseText(adoData)
		String jsonStr = new JsonBuilder(adoData).toPrettyString()
		//println jsonStr
		def eventData = adoData
		def changeSet = eventData.resource;
		if (adoData.resource && adoData.resource._links && adoData.resource._links.commits) {
			def commitsUrl = "${adoData.resource._links.commits.href}"
			
			def commits = codeManagementService.getCommits(commitsUrl)
			def locations = getPipelineChangeLocations(commits)
			if (locations.size() > 0) {
				String repoUrl = "${adoData.resource.repository.remoteUrl}"
				String name = "${adoData.resource.repository.name}"
				yamlExecutionService.runExecutableYaml(repoUrl,name, locations)
			}
		}
		return null
	}
	
	def getPipelineChangeLocations(def commits) {
		def locations = []
		for (def commit in commits.'value') {
			def changesUrl = "${commit._links.changes.href}"
			def changes = codeManagementService.getChanges(changesUrl)
			for (def change in changes.changes) {
				String path = "${change.item.path}"
				pipelineFolders.each { String pipelineFolder -> 
					if (path.contains("${pipelineFolder}") && path.endsWith('.yaml')) locations.add(path)
				}
			}
		}
		
		return locations
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
