package com.zions.vsts.services.distributedtask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

/**
 * @author z091556
 * 
 */
@Component
@Slf4j
public class TaskManagementService {
	
	@Autowired
	private IGenericRestClient genericRestClient;

	@Autowired
	ProjectManagementService projectManagementService

	public TaskManagementService() {
	}

	def writeTaskGroup(def collection, def project, def tgDef) {
		def body = new JsonBuilder(tgDef).toPrettyString()
		log.debug("TaskManagementService::writeTaskGroup --> ${body}")
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups",
				body: body,
				headers: [Accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true'],
		)
		return result
	}
	
	public def getTaskGroup(def collection, def project, String name) {
		log.debug("TaskManagementService::getTaskGroup -- name = " + name)
		def query = ['name':"*${name}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups",
				headers: [accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true'],
				query: query,
		)
		if (result == null || result.count == 0) {
			log.debug("TaskManagementService::getTaskGroup -- task group " + name + " not found. Returning NULL ...")
			return null
		}
		query = ['api-version':'5.1-preview.1']
		def result1 = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups/${result.value[0].id}",
				query: query,
		)
		return result1
	}

	public def getTaskGroupById(def collection, def project, String taskId) {
		log.debug("TaskManagementService::getTaskGroupById -- id = " + taskId)
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups/${taskId}",
				headers: [accept: 'application/json;api-version=6.0-preview.1;excludeUrls=true'],
		)
		if (result == null || result.count == 0) {
			log.debug("TaskManagementService::getTaskGroupById -- task group " + taskId + " not found. Returning NULL ...")
			return null
		}
		return result.value[0]
	}

	def ensureEnvironment(def collection, def project, def envName, def envDescr) {
		def result = getEnvironment(collection, project, envName)
		if (result == null) {
			//System.out.println("Adding environment ${envName} for project ${project.name}")
			System.out.println("Adding environment ${envName} for project ${project}")
			def env = ['name':"${envName}", 'description':"${envDescr}"]
			def body = new JsonBuilder(env).toPrettyString()
			log.debug("TaskManagementService::ensureEnvironment --> ${body}")
			result = genericRestClient.post(
					requestContentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/distributedtask/environments",
					body: body,
					headers: [Accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'],
			)
		}
		return result
	}
	
	def getEnvironment(def collection, def project, def envName) {
		def query = ['name':"${envName}", "api-version": "6.0-preview.1"]
		log.debug("TaskManagementService::getEnvironment --> ${envName}")
		def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/distributedtask/environments",
				query: query,
		)
		if (result.count < 1) {
			return null
		}
		System.out.println("Found existing environment ${envName} in project ${project}")
		return result.value[0]
	}
	
	public def updateBuildnumberRefs(def collection, def project, def newOutputVar) {
		def taskGroups = getTaskGroups(collection, project)
		taskGroups.value.each { taskGroup ->
			def result = updateBuildnumberRef(collection, project, taskGroup.id, newOutputVar)
			if (result == null) {
				log.debug("TaskManagementService::updateBuildnumberRefs -- Failed to update task group ${taskGroup.name}.")
			}
		}
		return
	}

	public def updateBuildnumberRef(def collection, def project, def taskGroupId, def outputVarName) {
		def query = ['api-version':'5.1-preview.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups/${taskGroupId}",
			query: query,
		)
		def taskGroup = result.value[0]
		// don't want call update if no changes were made
		def changed = false
		log.debug("TaskManagementService::updateBuildnumberRef -- Evaluating task group ${taskGroup.name} for project ${project.name}")
		def tasks = taskGroup.tasks
		tasks.each { taskSpec ->
			// Replace the currently used variable with 'build.buildnumber' for the XL Release start release task
			if (taskSpec.task.id == "6c731c3c-3c68-459a-a5c9-bde6e6595b5b") {
				def script = taskSpec.inputs.script
				def vNum = taskSpec.inputs.versionNumber
				if (script.contains("build.buildnumber")) {
					log.debug("Replacing build.buildnumber in script with '${outputVarName}' for XL Release start release task  ...")
					def newScript = script.replace("build.buildnumber", "${outputVarName}")
					taskSpec.inputs.script = newScript
					changed = true
				}
			}
		}
		// save changes
		if (changed) {
			return updateTaskGroupDef(collection, project, taskGroup)
		} else {
			return ""
		}
			
	}

	def updateTaskGroupDef(def collection, def project, def tgDef) {
		def body = new JsonBuilder(tgDef).toPrettyString()
		log.debug("TaskManagementService::updateTaskGroupDef --> ${body}")
		
		def result = genericRestClient.put(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups/${tgDef.id}",
			body: body,
			headers: [Accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true'],
		)
		return result
	}
	
	public def getTaskGroups(def collection, def project) {
		log.debug("TaskManagementService::getTasks for project = ${project.name}")
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups",
			headers: [accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true'],
		)
		if (result == null || result.count == 0) {
			log.debug("TaskManagementService::getTaskGroups -- No task groups found for project ${project.name}.")
		}
		return result
	}

	public def getTaskGroupById(def collection, def project, def id) {
		log.debug("TaskManagementService::getTaskGroupById -- ID = " + id)
		def query = ['api-version':'5.1-preview.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/taskgroups/${id}",
				query: query,
				)
		if (result == null) {
			log.debug("TaskManagementService::getTaskGroupById -- build with ID " + id + " not found. Returning NULL ...")
			return null
		}
		return result
	}

	public def getResource(String buildType, String phase, String resourceName) {
		def template = null
		def filename = "${buildType}-${phase}" 
		if (resourceName != null) {
			filename = resourceName
		}
		try {
			def s = getClass().getResourceAsStream("/build_templates/${filename}.json")
			JsonSlurper js = new JsonSlurper()
			template = js.parse(s)
		} catch (e) {
			log.debug("TaskManagementService::getResource -- Exception caught reading resource with name ${filename}.json not found. Returning NULL ...")
		}
		return template
	}

}