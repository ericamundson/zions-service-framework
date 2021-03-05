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
import com.zions.pipeline.services.db.PullRequestCompletedRepository
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.pipeline.services.db.PullRequestCompleted
import com.zions.pipeline.services.feedback.LogCallbackHandler

/**
 * ReST Controller for pipeline yaml service. 
 * @author z091182
 * 
 */
@Slf4j
@Component
class PipelineExecutionEndPoint implements MessageReceiverTrait, FeedbackTrait {

	//    @Autowired
	//    private PolicyManagementService policyManagementService;
	@Autowired
	CodeManagementService codeManagementService

	@Autowired
	YamlExecutionService yamlExecutionService
	
	@Autowired
	PullRequestCompletedRepository pullRequestCompletedRepository
	
	@Autowired
	LogCallbackHandler logCallbackHandler

	@Value('${pipeline.folders:.pipeline,pipeline}')
	String[] pipelineFolders

	@Value('${always.execute.folder:alwaysexecute}')
	String alwaysExecuteFolder
	
	@Value('${executables.folder:executables}')
	String executablesFolder
	
	

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
		if (adoData.resource && adoData.resource.lastMergeCommit) {
			String branch = "${adoData.resource.targetRefName}"
			String commitUrl = "${adoData.resource.lastMergeCommit.url}"
			String status = "${adoData.resource.status}".toLowerCase()
			if (status != 'completed') return null
			String comment = "${adoData.resource.completionOptions.mergeCommitMessage}"
			String pipelineId = getPipelineId(comment)
			logCallbackHandler.pipelineId = pipelineId
			String userName = getUserName(comment)
			String pullRequestId = "${adoData.resource.pullRequestId}"
			PullRequestCompleted prc = pullRequestCompletedRepository.findByPullRequestId(pullRequestId)
			if (prc) return null
			prc = new PullRequestCompleted([pullRequestId: pullRequestId, status: status])
			pullRequestCompletedRepository.save(prc)
			def commit = codeManagementService.getCommit(commitUrl)
			if (!commit) return null;
			String project = "${adoData.resource.repository.project.name}"
			String repo = "${adoData.resource.repository.name}"
			def locations = getPipelineChangeLocations(commit, project, repo, branch)
			if (locations.size() > 0) {
				String repoUrl = "${adoData.resource.repository.remoteUrl}"
				String name = "${adoData.resource.repository.name}"
				logContextStart(pipelineId, "Pull request on '${repo}' completed.")
				def result = yamlExecutionService.runExecutableYaml(repoUrl,name,locations, branch, project, pullRequestId, pipelineId, userName)
				logContextComplete(pipelineId, "Pull request on '${repo}' completed.")
				def issue = result.issue
				boolean hasRunXLBlueprint = result.hasRunXLBlueprint
				if (!issue && !hasRunXLBlueprint) {
					logContextStart(pipelineId, "Completed")
					logCompleted(pipelineId, "Blueprint processing is complete!")
					logContextComplete(pipelineId, "Completed")
				}				
				if (issue) {
					logContextStart(pipelineId, "Completed")
					logFailed(pipelineId, "Failed blueprint:  ${issue}")
					logContextComplete(pipelineId, "Completed")
					//sendFeedback(projectData, repoData, feedback, pullRequestId)
				}
			}
		}
		return null
	}
	
	String getPipelineId(String comment) {
		def pattern = /pipelineId:\s+(\S+)$/
		def matcher = comment =~ pattern
		if (matcher.size() == 1 && matcher[0].size() == 2) {
			return matcher[0][1]
		}
		return null
	}
	String getUserName(String comment) {
		def pattern = /userName:\s+(\S.+),/
		def matcher = comment =~ pattern
		if (matcher.size() == 1 && matcher[0].size() == 2) {
			return matcher[0][1]
		}
		return null
	}

	def getPipelineChangeLocations(def commit, String project, String repo, String branch) {
		def locations = []
		def changesUrl = "${commit._links.changes.href}"
		def changes = codeManagementService.getChanges(changesUrl)
		for (def change in changes.changes) {
			String path = "${change.item.path}"
			pipelineFolders.each { String pipelineFolder ->
				if (path.contains("${pipelineFolder}") && path.endsWith('.yaml') && !locations.contains(path)) {
					locations.add(path)
				}
			}
		}
		for (String pipelineFolder in pipelineFolders) {
			def regex = "(/${pipelineFolder}/${executablesFolder}/${alwaysExecuteFolder})\\S*(.yaml)\$"
			def fileList = codeManagementService.getFileList('', project, repo, regex, branch )
			for (String loc in fileList) {
				if (!locations.contains(loc)) {
					locations.add(loc)
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
