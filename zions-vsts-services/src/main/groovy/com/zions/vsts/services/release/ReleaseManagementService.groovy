package com.zions.vsts.services.release;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

@Component
@Slf4j
public class ReleaseManagementService {
	@Autowired
	@Value('${tfs.release.use.template:false}')
	private boolean useAdoTemplate
	
	@Autowired
	@Value('${tfs.release.generic.name:dev}')
	private String genericTemplateName
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService
	
	@Autowired
	private CodeManagementService codeManagementService
	
	@Autowired
	private BuildManagementService buildManagementService
	
	@Autowired
	private MemberManagementService memberManagementService

	@Autowired
	private EndpointManagementService endpointManagementService
	
	public ReleaseManagementService() {

	}
	
	public def ensureReleases(def collection, def project, def template, String xldEndpoint, String folder, def team) {
		def projectData = projectManagementService.getProject(collection, project, true)
		def repos = codeManagementService.getRepos(collection, projectData, team)
		repos.each { repo ->
			ensureRelease(collection, projectData, repo,  template, xldEndpoint, folder, team)
		}
	}
	
	public def ensureRelease(collection, projectData, repo, template, xldEndpoint, folder, team) {
		def release = getRelease(collection, projectData, repo.name, true)
		if (release == null) {
			release = createRelease(collection, projectData, repo, template, xldEndpoint, folder, team)
		}
	}
	
	public def ensureReleaseForBuild(def collection, def projectData, def repo, def buildId, def templateName) {
		def createdFlag = false
		def foundFlag = false
		log.debug("ReleaseManagementService::ensureReleaseForBuild -- Looking for existing release definition for ${repo.name}")
		def releaseDef = getRelease(collection, projectData, "${repo.name}", true)
		if (releaseDef == null) {
			log.debug("ReleaseManagementService::ensureReleaseForBuild -- Did NOT find an existing release definition for ${repo.name}")
			if (templateName == null) {
				templateName = this.genericTemplateName
			}
			def template = null
			// first check for ADO template release definition
			log.debug("ReleaseManagementService::ensureReleaseForBuild -- Loading ADO release template: " + templateName)
			template = getRelease(collection, projectData, templateName, true)
			if (template == null) {
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- ADO Template release definition not found for project ${projectData.name}")
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- Using resource file.  File name: "+ templateName + ".json")
				template = getTemplateAsResource(templateName)
				// indicate we're using a resource file
				this.useAdoTemplate = false
			}

			if (template == null) {
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- No usable template found. Unable to create release definition")
				//return null
			}

			releaseDef = createReleaseForBuild(collection, projectData, repo, template, buildId)
		} else {
			// found existing release definition for the repo
			foundFlag = true
		}
		//return releaseDef
		def relDefName = ""
		if (releaseDef != null && !foundFlag) {
			createdFlag = true
			relDefName = "${releaseDef.name}"
		}
		def returnObject = [relDefCreated: createdFlag,
							relDefFound: foundFlag,
							releaseDefName: relDefName]

		return returnObject
		
	}

	private def createReleaseForBuild(collection, project, repo, template, buildId) {

		log.debug("ReleaseManagementService::createReleaseForBuild -- Getting release build for ${repo.name}. Build ID: "+ buildId)
		def buildDef = buildManagementService.getBuildById(collection, project, buildId)
		if (buildDef == null) {
			log.debug("ReleaseManagementService::createReleaseForBuild -- Unable to retrieve release build for ${repo.name}. Returning null ...")
			return null
		}
		// how will we get team data ?? 
		//def team = ""
		template.id = -1
		template.name = repo.name
		// not using folders
		template.path = "\\"
		template.description = "Release definition for ${repo.name}"
		template.artifacts[0].alias = "_${buildDef.name}"
		template.artifacts[0].definitionReference.artifactSourceDefinitionUrl.id = buildDef._links.web.href
		template.artifacts[0].definitionReference.definition.id = buildDef.id
		template.artifacts[0].definitionReference.definition.name = buildDef.name
		template.artifacts[0].definitionReference.project.id = project.id
		template.artifacts[0].definitionReference.project.name = project.name
		template.artifacts[0].definitionReference.sourceId = "${project.id}:${buildDef.id}"
		// set variables for uDeploy (udclient) task
		template.environments.each { env ->
			env.deployPhases.each { phase ->
				phase.deploymentInput.artifactsDownloadInput.downloadInputs.each { input ->
					if ("${input.artifactType}" == "Build") {
						input.alias = "_${buildDef.name}"
					}
				}
				phase.workflowTasks.each { task ->
					if ("${task.taskId}" == '55049627-78d6-4a04-84e9-e136fd98bf78') {
						task.inputs.base = "${task.inputs.base}".replace("%alias_name%", "_${buildDef.name}")
						task.inputs.component = "${repo.name}"
					}
				}
			}
			//env.owner = teamData
		}
		template.triggers.each { trigger ->
			if ("${trigger.triggerType}" == "artifactSource" || "${trigger.triggerType}" == "1") {
				trigger.artifactAlias = "_${buildDef.name}"
			}
		}
		if (!this.useAdoTemplate) {
			def queueData = buildManagementService.getQueue(collection, project, "${buildManagementService.queue}")
			if (queueData != null) {
				template.environments.each { env ->
					env.deployPhases.each { phase ->
						phase.deploymentInput.queueId = queueData.id
					}
				}
			}
		}
		log.debug("ReleaseManagementService::createReleaseForBuild -- Calling writeReleaseDefinition for ${repo.name} ...")
		return writeReleaseDefinition(collection, project, template)
	}

	private def ensureDRRelease(collection, projectData, repo, buildId) {
		def createdFlag = false
		def foundFlag = true
		// Look for existing DR release for repo
		def release = getRelease(collection, projectData, repo.name + "-dr", true)
		if (release == null) {
			foundFlag = false
			// use existing release definition as template
			def template = getRelease(collection, projectData, repo.name, true)
			if (template == null) {
				log.debug("ReleaseManagementService::ensureDRRelease -- Unable to retrieve DR release definition for ${repo.name}. Returning null ...")
			} else {
				release = createDRRelease(collection, projectData, repo, template, buildId)
			}
		}
		def relDefName = ""
		if (release != null && !foundFlag) {
			createdFlag = true
			relDefName = "${releaseDef.name}"
		}
		def returnObject = [relDefCreated: createdFlag,
			relDefFound: foundFlag,
			releaseDefName: relDefName]

		return returnObject
	}
	
	public def createRelease(collection, project, repo, template, xldEndpoint, folder, team) {
		def teamData = memberManagementService.getTeam(collection, project, team)
		def buildDef = buildManagementService.getBuild(collection, project, "${repo.name}-Release")
		if (buildDef == null) return null
		def endpoint = endpointManagementService.getServiceEndpoint(collection, project.id, xldEndpoint)
		if (endpoint == null) return null
		template.id = -1
		template.name = repo.name
		template.path = folder
		template.description = "Release definition for ${repo.name}"
		template.artifacts[0].alias = buildDef.name
		template.artifacts[0].definitionReference.artifactSourceDefinitionUrl.id = buildDef._links.web.href
		template.artifacts[0].definitionReference.definition.id = buildDef.id
		template.artifacts[0].definitionReference.definition.name = buildDef.name
		template.artifacts[0].definitionReference.project.id = project.id
		template.artifacts[0].definitionReference.project.name = project.name
		template.artifacts[0].definitionReference.sourceId = "${project.id}:${buildDef.id}"
		template.environments.each { env ->
			env.deployPhases.each { phase ->
				phase.workflowTasks.each { task ->
					if ("${task.taskId}" == '589dce45-4881-4410-bcf0-1afbd0fc0f65') {
						task.inputs.connectedServiceName = endpoint.id
						task.inputs.targetEnvironment = "Environments/${project.name}/${repo.name}/${env.name}"
					}
				}
			}
			env.owner = teamData
		}
		def queueData = buildManagementService.getQueue(collection, project, "${buildManagementService.queue}")
		if (queueData != null) {
			template.environments.each { env ->
				env.deployPhases.each { phase ->
					phase.deploymentInput.queueId = queueData.id
				}
			}
	
		}
		return writeReleaseDefinition(collection, project, template)
	}

	private def createDRRelease(collection, project, repo, template, buildId) {

		log.debug("ReleaseManagementService::createDRRelease -- Getting DR release build for ${repo.name}. Build ID: "+ buildId)
		def buildDef = buildManagementService.getBuildById(collection, project, buildId)
		if (buildDef == null) {
			log.debug("ReleaseManagementService::createDRRelease -- Unable to retrieve DR release build for ${repo.name}. Returning null ...")
			return null
		}
		// how will we get team data ?? 
		//def team = ""
		template.id = -1
		template.name = repo.name + "-dr"
		// not using folders
		template.path = "\\"
		template.description = "DR Release definition for ${repo.name}"
		template.artifacts[0].alias = "_${buildDef.name}"
		template.artifacts[0].definitionReference.artifactSourceDefinitionUrl.id = buildDef._links.web.href
		template.artifacts[0].definitionReference.definition.id = buildDef.id
		template.artifacts[0].definitionReference.definition.name = buildDef.name
		template.artifacts[0].definitionReference.project.id = project.id
		template.artifacts[0].definitionReference.project.name = project.name
		template.artifacts[0].definitionReference.sourceId = "${project.id}:${buildDef.id}"
		// set variables for uDeploy (udclient) task
		template.environments.each { env ->
			env.deployPhases.each { phase ->
				phase.deploymentInput.artifactsDownloadInput.downloadInputs.each { input ->
					if ("${input.artifactType}" == "Build") {
						input.alias = "_${buildDef.name}"
					}
				}
				phase.workflowTasks.each { task ->
					if ("${task.taskId}" == '55049627-78d6-4a04-84e9-e136fd98bf78') {
						task.inputs.base = "${task.inputs.base}".replace("%alias_name%", "_${buildDef.name}")
						task.inputs.component = "${repo.name}"
					}
				}
			}
			//env.owner = teamData
		}
		template.triggers.each { trigger ->
			if ("${trigger.triggerType}" == "artifactSource" || "${trigger.triggerType}" == "1") {
				trigger.artifactAlias = "_${buildDef.name}"
			}
		}
		// set pool to On-Prem DR, same as DR build def
		def queueData = buildDef.queue
		if (queueData != null) {
			template.environments.each { env ->
				env.deployPhases.each { phase ->
					phase.deploymentInput.queueId = queueData.id
				}
			}
		}
		log.debug("ReleaseManagementService::createReleaseForBuild -- Calling writeReleaseDefinition for ${repo.name} ...")
		return writeReleaseDefinition(collection, project, template)
	}

	def writeReleaseDefinition(collection, project, template) {
		log.debug("ReleaseManagementService::writeReleaseDefinition -- Saving Release Definition with name ${template.name}")
		def body = new JsonBuilder(template).toPrettyString()
		log.debug("ReleaseManagementService::writeReleaseDefinition --> ${body}")

		def releaseUri = getReleaseApiUrl(collection, project)
		log.debug("ReleaseManagementService::writeReleaseDefinition -- URI = ${releaseUri}/definitions")
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${releaseUri}/definitions",
			body: body,
			headers: [Accept: 'application/json;api-version=4.1-preview.3;excludeUrls=true'],
			)
		return result
	}
	
	public def getRelease(def collection, def project, String name, boolean exactMatch = true) {
		log.debug("ReleaseManagementService::getRelease -- Looking for Release Definition with name ${name}")
		def query = ['api-version':'6.0','searchText':"${name}",isExactNameMatch:exactMatch]
		def releaseUri = getReleaseApiUrl(collection, project)
		log.debug("ReleaseManagementService::getRelease -- URI = ${releaseUri}/definitions")
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${releaseUri}/definitions",
				query: query,
				)
		//log.debug("ReleaseManagementService::getRelease -- Result: "+ result)
		if (result == null || result.count == 0) {
			log.debug("ReleaseManagementService::getRelease -- Release Definition ${name} NOT found")
			return null
		}
		query = ['api-version':'6.0']
		def result1 = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${releaseUri}/definitions/${result.value[0].id}",
				query: query,
				)
		//query = ['api-version':'4.1', 'propertyFilters':'processParameters']
		return result1
	}

	def ensureReleaseFolder(def collection, def project, String folder) {
		//def projectData = projectManagementService.getProject(collection, project, true)
		def folders = folder.split('\\\\')
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		int i = 0
		String cFolder = ""
		folders.each { folderName ->
			if (i != 0) {
				cFolder = "${cFolder}\\${folderName}"
				def folderObj = [createdBy: null, createdOn: null, lastChangedBy: null, lastChangedDate: null, path: "${cFolder}"]
				def efolder = URLEncoder.encode(cFolder, 'utf-8')
				efolder = efolder.replace('+', '%20')
		
				def body = new JsonBuilder(folderObj).toPrettyString()
				def releaseUri = getReleaseApiUrl(collection, project)
				def result = genericRestClient.post(
					requestContentType: ContentType.JSON,
					uri: "${releaseUri}/folders/${efolder}",
					body: body,
					headers: [accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'],
					)
				if (result != null) {
					def nfolder = result.toString()
				}
			}
			i++
		}
	}

	public def getReleases(def collection, def project, def query = null) {
		log.debug("ReleaseManagementService::getReleases for project = ${project.name}")
		//def query = ['name':"*${name}"]
		def releaseUri = getReleaseApiUrl(collection, project)
		def result = null
		if (query == null) {
			result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${releaseUri}/definitions",
				headers: [accept: 'application/json;api-version=7.0;excludeUrls=true'],
			)
		} else {
			result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${releaseUri}/definitions",
				query: query,
				headers: [accept: 'application/json;api-version=7.0;excludeUrls=true'],
			)

		}
		if (result == null || result.count == 0) {
			log.debug("ReleaseManagementService::getReleases -- No release defs found for project ${project.name}.")
			return null
		}
		return result.'value'
	}
	public def getReleasesExecutions(def collection, def project, def rDef) {
		def releaseUri = getReleaseApiUrl('', project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${releaseUri}/releases",
			query: [definitionId: "${rDef.id}"],
			headers: [accept: 'application/json;api-version=5.1;excludeUrls=true']
		)
		if (result == null) {
			return []
		}
		return result.'value'
	}
	public def getReleasesViaQuery(def collection, def project, def query) {
		log.debug("ReleaseManagementService::getReleases for project = ${project.name}")
		//def query = ['name':"*${name}"]
		def releaseUri = getReleaseApiUrl(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${releaseUri}/definitions",
			query: query,
			headers: [accept: 'application/json;api-version=5.1;excludeUrls=true'],
		)
		def releases = []
		if (result == null || result.count == 0) {
			log.debug("ReleaseManagementService::getReleases -- No release defs found for project ${project.name}.")
		}
		
		return result.'value'
	}

	public def updateReleases(def collection, def project, boolean deleteUnwantedTasks) {
		def releaseDefs = getReleases(collection, project)
		releaseDefs.each { releaseDef ->
			if (!releaseDef.name.startsWith("d3") && !releaseDef.name.startsWith("zbc")) {
				def result = updateRelease(collection, project, releaseDef.id, deleteUnwantedTasks)
				if (result == null) {
					log.debug("ReleaseManagementService::updateRelease -- Failed to update release def ${releaseDef.name}.")
				}
			}
		}
		return
	}

	public def updateRelease(def collection, def project, def releaseId, boolean deleteUnwantedTasks) {
		log.debug("ReleaseManagementService::updateRelease -- Evaluating release def ${releaseId} for project ${project.name}")
		def query = ['api-version':'5.1']
		def releaseUri = getReleaseApiUrl(collection, project)
		def releaseDef = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${releaseUri}/definitions/${releaseId}",
			query: query,
		)
		// don't want call update if no changes were made
		def changed = false
		releaseDef.environments.each { environment ->
			// skip non-deployment phases
			if (environment.name != "Start Isolation" && environment.name != "Finalize Release") {
				environment.deployPhases.each { phase ->
					boolean xldtaskfound = false
					def tasks = phase.workflowTasks
					// set new values ,etc.
					for (int idx = 0; idx < tasks.size(); idx++) {
					//environment.deployPhases.tasks.each { step ->
						def task = tasks[idx]
						// remove UDeploy: Ant deploy-application task
						if (task.taskId == "3a6a2d63-f2b2-4e93-bcf9-0cbe22f5dc26" && task.inputs.targets == "deploy-application") {
							if (deleteUnwantedTasks) {
								log.debug("Removing uDeploy Ant task for environment ${environment.name} ...")
								tasks.remove(idx)
								idx--
								changed = true
							} else {
								if (task.enabled == true) {
									log.debug("Disabling uDeploy Ant task for environment ${environment.name} ...")
									task.enabled = false
									changed = true
								}
							}
						}
						// remove UDeploy: Copy Files task for udclient_runAppProc.json file
						if (task.taskId == "a8515ec8-7254-4ffd-912c-86772e2b5962" && task.inputs.targetFiles == "udclient_runAppProc.json") {
							if (deleteUnwantedTasks) {
								log.debug("Removing Copy Files task for udclient_runAppProc.json file for environment ${environment.name} ...")
								tasks.remove(idx)
								idx--
								changed = true
							} else {
								if (task.enabled == true) {
									log.debug("Disabling Copy Files task for udclient_runAppProc.json file for environment ${environment.name} ...")
									task.enabled = false
									changed = true
								}
							}
						}
						// Check for XL Deploy task
						if (task.taskId == "589dce45-4881-4410-bcf0-1afbd0fc0f65") {
							log.debug("Found XL Deploy task for environment ${environment.name} ...")
							xldtaskfound = true
							// remove if incorrectly added for Start Release Isolation phase
							if (environment.name == "Start Release Isolation") {
								log.debug("Removing XL Deploy task for environment ${environment.name} ...")
								tasks.remove(idx)
								idx--
								changed = true
							} else {
								// Make sure task is enabled
								if (task.enabled == false) {
									log.debug("Enabling XL Deploy task ...")
									task.enabled = true
									changed = true
								}
								// Set the target environment ??
								def target = task.inputs.targetEnvironment
								if (target != "Environments/Digital Banking/${environment.name}") {
									log.debug("Setting target environment for XL Deploy task to 'Environments/Digital Banking/${environment.name}'...")
									task.inputs.targetEnvironment = "Environments/Digital Banking/${environment.name}"
									changed = true
								}
							}
						}
						// disable or remove the uDeploy task group for the project
						if (task.taskId == "3c199b71-a326-4735-8ddb-282122af20c4") {
							if (deleteUnwantedTasks) {
								log.debug("Removing uDeploy: create/upload version task group for environment ${environment.name} ...")
								tasks.remove(idx)
								idx--
								changed = true
							} else {
								if (task.enabled == true) {
									log.debug("Disabling uDeploy create/upload version task group for environment ${environment.name} ...")
									task.enabled = false
									changed = true
								}
							}
						}
					}
					// Add XL Deploy task if not found
					if (!xldtaskfound) {
						// add the XL Deploy task
						log.debug("Adding XL Deploy task for environment ${environment.name} ...")
						//def connSvcId = "5014df84-64bf-45a3-9bf8-d9ccbbab5447"
						//def connSvcId = "41cc9a0a-c264-41b7-bb1c-5832cfe55e1a"
						def jsonSlurper = new JsonSlurper()
						def xldtask = jsonSlurper.parseText '''
							{ "environment": {},
							  "taskId": "589dce45-4881-4410-bcf0-1afbd0fc0f65",
							  "version": "7.*",
							  "name": "XL Deploy - Import and Deploy",
							  "refName": "",
							  "enabled": true,
							  "alwaysRun": false,
							  "continueOnError": false,
							  "timeoutInMinutes": 0,
							  "definitionType": "task",
							  "overrideInputs": {},
							  "condition": "succeeded()",
							  "inputs": {
								"connectedServiceName": "41cc9a0a-c264-41b7-bb1c-5832cfe55e1a",
								"darPackage": "**/*.dar",
								"targetEnvironment": "",
								"rollback": "false"}
							}'''
						xldtask.inputs.targetEnvironment = "Environments/Digital Banking/${environment.name}"
						tasks.add(xldtask)
						changed = true
					}
				}
			}
		}
		// save changes
		if (changed) {
			return updateReleaseDefinition(collection, project, releaseDef)
		} else {
			return ""
		}
	}

	def updateReleaseDefinition(def collection, def project, def relDef) {
		def body = new JsonBuilder(relDef).toPrettyString()
		log.debug("ReleaseManagementService::updateReleaseDefinition --> ${body}")
		//System.out.println("updateReleaseDefinition --> ${body}")
		
		def releaseUri = getReleaseApiUrl(collection, project)
		log.debug("ReleaseManagementService::updateReleaseDefinition -- URI = ${releaseUri}/definitions/${relDef.id}")
		def result = genericRestClient.put(
			requestContentType: ContentType.JSON,
			uri: "${releaseUri}/definitions/${relDef.id}",
			body: body,
			headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true'],
		)
		return result
	}
	
	def getReleaseApiUrl(def collection, def project) {
		def baseUri = genericRestClient.getTfsUrl()
		if ("${baseUri}".contains("visualstudio.com")) {
			baseUri = baseUri.replace('visualstudio.com', 'vsrm.visualstudio.com')
		} else {
			baseUri = baseUri.replace('dev.azure.com', 'vsrm.dev.azure.com')
		}
		def releaseApi = "/${collection}/${project.id}/_apis/release".replace('//', '/')
		return "${baseUri}${releaseApi}"
	}

	public def getTemplateAsResource(String resourceName) {
		def template = null
		try {
			def s = getClass().getResourceAsStream("/release_templates/${resourceName}.json")
			JsonSlurper js = new JsonSlurper()
			template = js.parse(s)
		} catch (e) {
			log.debug("ReleaseManagementService::getTemplateAsResource -- Exception caught reading resource with name ${resourceName}.json not found. Returning NULL ...")
		}
		return template
	}

}
