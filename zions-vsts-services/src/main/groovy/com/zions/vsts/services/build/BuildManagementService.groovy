package com.zions.vsts.services.build;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
@Slf4j
public class BuildManagementService {
	
	@Autowired
	@Value('${tfs.build.use.template}')
	private boolean useTfsTemplate
	
	@Autowired
	@Value('${tfs.build.queue}')
	private String queue

	@Autowired
	private IGenericRestClient genericRestClient;

	@Autowired
	private CodeManagementService codeManagementService

	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	MemberManagementService memberManagementService

	public BuildManagementService() {
	}

	public def provideTag(def buildData) {
	}
	
	def ensureBuildFolder(def collection, def project, String folder) {
		def projectData = projectManagementService.getProject(collection, project, true)
		def efolder = URLEncoder.encode(folder, 'utf-8')
		efolder = efolder.replace('+', '%20')
		
		def folderObj = [description: '', path: "\\${folder}"]
		
		def body = new JsonBuilder(folderObj).toPrettyString()
		def result = genericRestClient.put(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}//_apis/build/folders/${efolder}",
			body: body,
			headers: [Accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'],
			)
		return result
		
	}

	public def detectBuildType(def collection, def project, def repo) {
		log.debug("BuildManagementService::detectBuildType -- Calling codeManagementService.listTopLevel ...")
		def topFiles = codeManagementService.listTopLevel(collection, project, repo)
		def buildType = BuildType.NONE
		if (topFiles != null) {
			log.debug("BuildManagementService::detectBuildType -- Found top-level files ...")
			//topFiles.value.each { file ->
			for (def file in topFiles.value) {
				log.debug("BuildManagementService::detectBuildType -- Looking for build properties file ...")
				def path = file.path
				if ("${path}".endsWith('build.gradle')) {
					buildType = BuildType.GRADLE
					break
				} else if ("${path}".endsWith('pom.xml')) {
					buildType = BuildType.MAVEN
					break
				} else if ("${path}".endsWith('package.json')) {
					buildType = BuildType.NODE
					break
				} else if ("${path}".endsWith('build.xml')) {
					buildType = BuildType.ANT
					break
				}
			}
		}
		log.debug("BuildManagementService::detectBuildType -- Returning buildType of "+buildType)
		return buildType
	}

	public def ensureBuilds(def collection, def project, def folder, def team) {
		def projectData = projectManagementService.getProject(collection, project, true)
		def repos = codeManagementService.getRepos(collection, projectData, team)
		repos.each { repo ->
			def buildType = detectBuildType(collection, projectData, repo)
			if (buildType != BuildType.NONE) {
				codeManagementService.ensureDeployManifest(collection, projectData, repo)
				def bd = ensureBuild(collection, projectData, repo, buildType, 'CI', folder)
				ensureBuild(collection, projectData, repo, buildType, 'Release', folder)
				
			}
		}
	}
	
	public def ensureBuildsForBranch(def collection, def projectData, def repo) {
		def buildType = detectBuildType(collection, projectData, repo)
		log.debug("BuildManagementService::ensureBuildsForBranch -- Detected BuildType = ${buildType}")
		if (buildType == BuildType.NONE) {
			return null
		}
		log.debug("BuildManagementService::ensureBuildsForBranch -- Calling get CI Build ...")
		def ciBd = null
		def build = getBuild(collection, projectData, repo, 'CI')
		if (build.count == 0) {
			build = createBuild(collection, projectData, repo, buildType, 'CI')
			if (build == null ) {
				log.error("BuildManagementService::ensureBuildsForBranch -- CI Build creation failed!")
			}
			ciBd = build
		} else {
			ciBd = build.value[0]
		}
		//ciBd = ensureBuild(collection, projectData, repo, buildType, 'CI')
		log.debug("BuildManagementService::ensureBuildsForBranch -- CI Build Done: "+ciBd)
		def relBd = null
		def build1 = getBuild(collection, projectData, repo, 'Release')
		if (build1.count == 0) {
			relBd = createBuild(collection, projectData, repo, buildType, 'Release')
			if (relBd == null ) {
				log.error("BuildManagementService::ensureBuildsForBranch -- Release Build creation failed!")
			}
		}
		//def relBd = ensureBuild(collection, projectData, repo, buildType, 'Release')
		log.debug("BuildManagementService::ensureBuildsForBranch -- ensure Release Done: "+relBd)

		return ciBd
	}
	
	def reviseReleaseLabels(def collection, def projectData, String repoList, String releaseLabel) {
		def repos = codeManagementService.getRepos(collection, projectData)
		def repoNames = repoList.split(',')
		if (repoNames.length > 0 && "${repoNames[0]}" == 'all' ) {
			repos.value.each { repo ->
				reviseReleaseLabel(collection, projectData, repo, releaseLabel)
			}
		} else {
			repos.value.each { repo ->
				if (repoNames.contains("${repo.name}")) {
					reviseReleaseLabel(collection, projectData, repo, releaseLabel)
				}
			}

		}
	}
	def reviseReleaseLabel(def collection, def projectData, def repo, releaseLabel) {
		def build = getBuild(collection, projectData, "${repo.name}-Release")
		if (build == null) return null
		build.process.phases.each { phase ->
			phase.steps.each { step ->
				if ("${step.task.id}" == '218eff04-a485-4087-b005-e1f04527654d') {
					step.inputs.InitialVersionPrefix = releaseLabel
				}
			}
		}
		def body = new JsonBuilder(build).toPrettyString()
		def result = genericRestClient.put(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/build/definitions/${build.id}",
			body: body,
			headers: [Accept: 'application/json;api-version=4.1;excludeUrls=true'],
			)

	}
	public def ensureBuild(def collection, def project, def repo, BuildType buildType, def buildStage, def folder) {
		def build = getBuild(collection, project, repo, buildStage)
		if (build.count == 0) {
			String name = buildType.toString().toLowerCase()
			build = createBuild(collection, project, repo, buildType, buildStage, folder)
			if (build != null && "${buildStage}" == 'CI') {
				branchPolicy(collection, project, repo, build, 'refs/heads/master')
			}
		}
		return build
	}
	
	def branchPolicy(def collection, def project, def repo, def ciBuild, def branch) {
		def policy = [id: -2, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
			settings:[buildDefinitionId: ciBuild.id, name: "${repo.name} validation", manualQueueOnly: false, queueOnSourceUpdateOnly:true,
				scope:[[matchKind: 'Exact',refName: branch, repositoryId: repo.id]], validDuration: 720],
			type: [id: "0609b952-1397-4640-95ec-e00a01b2c241"]]
		def body = new JsonBuilder(policy).toPrettyString()
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/policy/Configurations",
				body: body,
				headers: [Accept: 'application/json;api-version=3.2;excludeUrls=true'],
				)
	}

	public def createBuild(def collection, def project, def repo, BuildType buildType, String buildStage, def folder) {
		def bDef = null
		if (this.useTfsTemplate) {
			// Looking for template build definition with name like 'maven-CI-template', 'gradle-Release-template', 'ant-CI-template', etc.
			String templateName = buildType.toString().toLowerCase()+"-"+buildStage+"-template"
			log.debug("BuildManagementService::createBuild -- Using TFS template: "+templateName)
			bDef = getBuild(collection, project, templateName)
		} else {
			log.debug("BuildManagementService::createBuild -- Using local resource file.  Build type: "+buildType.toString().toLowerCase()+", build stage: "+ buildStage)
			bDef = getResource(buildType.toString().toLowerCase(), buildStage)
		}
		if (bDef == null) {
			log.debug("BuildManagementService::createBuild -- Build definition template not found. Returning NULL ...")
			return null
		}
		bDef.remove('authoredBy')
		bDef.name = "${repo.name}-${buildStage}"
		bDef.id = -1
		bDef.draftOf = null
		bDef.path = "${folder}"
		bDef.counters = [:]
		bDef.comment = "${buildStage} build for ${repo.name}"
		bDef.createdDate = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		bDef.project = project
		bDef.badgeEnabled = false
		bDef.demands = []
		bDef.variableGroups = []
		bDef.properties = [source: 'AllDefinitions']
//		bDef.quality = 1
//		bDef.queueStatus = 0
//		bDef.type = 2
//		bDef.jobAuthorizationScope = 1
//		bDef.triggers = []
		bDef.project = project
//		bDef.project.id = project.id
//		bDef.project.name = project.name
//		bDef.project.url = project.url
//		bDef.project.description = project.description
//		bDef.project.state = project.state
//		bDef.project.revision = project.revision
//		bDef.project.visibility = project.visibility
		
		bDef.repository.id = "${repo.id}"
		bDef.repository.name = "${repo.name}"
		bDef.repository.url = "${repo.url}"
		bDef.repository.defaultBranch = "${repo.defaultBranch}"
		bDef.retentionSettings = getRetentionSettings(collection)
		def queueData = getQueue(collection, project, "${this.queue}")
		if (queueData != null) {
			bDef.queue = queueData
			bDef.process.phases.each { phase ->
				phase.target.queue = queueData
			}
		}
		//def memberData = memberManagementService.getMember(collection, 'z091182')
		def body = new JsonBuilder(bDef).toPrettyString()
		
//		File f = new File("${repo.name}-${buildStage}.json")
//		def o = f.newDataOutputStream()
//		o << body
//		o.close()
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				body: body,
				headers: [Accept: 'application/json;api-version=4.1;excludeUrls=true'],
				)
	}
	
	public def getQueue(String collection, def project, String name) {
		def query = ['name':"${name}", ]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/distributedtask/queues",
				query: query,
				headers: [Accept: 'application/json;api-version=4.1-preview.1;excludeUrls=true'],
				)
		def theQueue = null
		result.value.each { queue ->
			if ("${name}" == "${queue.name}") {
				theQueue = queue
			}
		}
		return theQueue

	}
	
	public def getRetentionSettings(String collection) {
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/build/settings",
				headers: [Accept: 'application/json','api-version': '4.1'],
				)
		return result
	}

	public def getBuild(def collection, def project, def repo, def qualifier) {
		log.debug("BuildManagementService::getBuild -- buildName = "+repo.name+"-"+qualifier)
		def query = ['api-version':'4.1','name':"${repo.name}-${qualifier}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
		return result
	}
	
	public def getBuild(def collection, def project, String name) {
		log.debug("BuildManagementService::getBuild -- name = " + name)
		def query = ['name':"*${name}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				headers: [accept: 'application/json;api-version=5.0-preview.6;excludeUrls=true'],
				query: query,
				)
		if (result == null || result.count == 0) {
			log.debug("BuildManagementService::getBuild -- build " + name + " not found. Returning NULL ...")
			return null
		}
		query = ['api-version':'4.1']
		def result1 = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${result.value[0].id}",
				query: query,
				)
		query = ['api-version':'4.1', 'propertyFilters':'processParameters']
		return result1
	}

	public def getResource(String buildType, String phase) {
		def template = null
		try {
		def s = getClass().getResourceAsStream("/build_templates/${buildType}-${phase}.json")
		JsonSlurper js = new JsonSlurper()
		template = js.parse(s)
		} catch (e) {}
		return template
	}
}

enum BuildType {
	NONE, GRADLE, ANT, MAVEN, NODE
}
