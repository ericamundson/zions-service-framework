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
	@Value('${tfs.release.generic.name:}')
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
		def release = getRelease(collection, projectData, repo.name)
		if (release == null) {
			release = createRelease(collection, projectData, repo, template, xldEndpoint, folder, team)
		}
	}
	
	public def ensureReleaseForBuild(def collection, def projectData, def repo, def buildId, boolean isDRBranch, def templateName) {
		// if this is for a DR branch, call special operation
		if (isDRBranch) {
			return ensureDRRelease(collection, projectData, repo, buildId)
		}
		log.debug("ReleaseManagementService::ensureReleaseForBuild -- Looking for existing release definition for ${repo.name}")
		def releaseDef = getRelease(collection, projectData, "${repo.name}")
		if (releaseDef == null) {
			log.debug("ReleaseManagementService::ensureReleaseForBuild -- Did NOT find an existing release definition for ${repo.name}")
			def template = null
			if (templateName == null) {
				templateName = this.genericTemplateName
			}
			// retrieve the template release definition
			if (this.useAdoTemplate) {
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- Loading ADO release template: " + templateName)
				template = getRelease(collection, projectData, templateName)
			} else {
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- Using local resource file.  File name: "+ templateName)
				template = getTemplateAsResource(templateName)
			}

			if (template == null) {
				log.debug("ReleaseManagementService::ensureReleaseForBuild -- Template release definition not found for project ${projectData.name}")
				return null
			}

			return createReleaseForBuild(collection, projectData, repo, template, buildId)
		}
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
			if ("${trigger.triggerType}" == "artifactSource") {
				trigger.artifactAlias = "_${buildDef.name}"
			}
		}
		def queueData = buildManagementService.getQueue(collection, project, "${buildManagementService.queue}")
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

	private def ensureDRRelease(collection, projectData, repo, buildId) {
		// Look for existing DR release for repo
		def release = getRelease(collection, projectData, repo.name + "-dr")
		if (release == null) {
			// use existing release definition as template
			def template = getRelease(collection, projectData, repo.name)
			release = createDRRelease(collection, projectData, repo, template, buildId)
		}
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

	def writeReleaseDefinition(collection, project, template) {
		log.debug("ReleaseManagementService::writeReleaseDefinition -- Saving Release Definition with name ${template.name}")
		def body = new JsonBuilder(template).toPrettyString()
		//log.debug("ReleaseManagementService::writeReleaseDefinition -- Request body = ${body}")

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
	
	public def getRelease(def collection, def project, String name) {
		log.debug("ReleaseManagementService::getRelease -- Looking for Release Definition with name ${name}")
		def query = ['api-version':'4.1-preview.3','searchText':"${name}",isExactNameMatch:true]
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
		query = ['api-version':'4.1-preview.3']
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
