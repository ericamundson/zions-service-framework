package com.zions.vsts.services.build;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
public class BuildManagementService {

	@Autowired
	private GenericRestClient genericRestClient

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

	public def detectBuildType(def collection, def project, def repo) {
		def topFiles = codeManagementService.listTopLevel(collection, project, repo)
		def buildType = BuildType.NONE
		if (topFiles != null) {
			topFiles.value.each { file ->
				def path = file.path
				if ("${path}".endsWith('build.gradle')) {
					buildType = BuildType.GRADLE
				} else if ("${path}".endsWith('pom.xml')) {
					buildType = BuildType.MAVEN
				} else if ("${path}".endsWith('package.json')) {
					buildType = BuildType.NODE
				} else if ("${path}".endsWith('build.xml')) {
					buildType = buildType.ANT
				}
			}
		}
		return buildType
	}

	public def ensureBuilds(def collection, def project) {
		def projectData = projectManagementService.getProject(collection, project, true)
		def repos = codeManagementService.getRepos(collection, projectData)
		repos.value.each { repo ->
			def buildType = detectBuildType(collection, projectData, repo)
			if (buildType != BuildType.NONE) {
				//ensureBuild(collection, projectData, repo, buildType, 'CI')
				ensureBuild(collection, projectData, repo, buildType, 'Release')
			}
		}
	}

	public def ensureBuild(def collection, def project, def repo, BuildType buildType, buildStage) {
		def build = getBuild(collection, project, repo, buildStage)
		if (build.count == 0) {
			String name = buildType.toString().toLowerCase()
			createBuild(collection, project, repo, buildType, buildStage)
		}
	}

	public def createBuild(def collection, def project, def repo, BuildType buildType, String buildStage) {
		def bDef = getResource(buildType.toString().toLowerCase(), buildStage)
		if (bDef == null) return null
		bDef.remove('authoredBy')
		bDef.name = "${repo.name}-${buildStage}"
		bDef.id = -1
		bDef.draftOf = null
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
		def queueData = getQueue(collection, project, 'Default')
		if (queueData.count > 0) {
			bDef.queue = queueData.value[0]
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
		return result

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
		def query = ['api-version':'4.1','name':"${repo.name}-${qualifier}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
		return result
	}
	
	public def getBuild(def collection, def project, String name) {
		def query = ['api-version':'4.1','name':"${name}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
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
