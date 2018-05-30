package com.zions.vsts.services.build;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
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
		def projectData = projectManagementService.getProject(collection, project)
		def repos = codeManagementService.getRepos(collection, projectData)
		repos.value.each { repo ->
			def buildType = detectBuildType(collection, projectData, repo)
			if (buildType != BuildType.NONE) {
				ensureCIBuild(collection, projectData, repo, buildType)
			}
		}
	}

	public def ensureCIBuild(def collection, def project, def repo, BuildType buildType) {
		def build = getBuild(collection, project, repo, 'CI')
		if (build.count == 0) {
			String name = buildType.toString().toLowerCase()
			createCIBuild(collection, project, repo, buildType)
		}
	}

	public def createCIBuild(def collection, def project, def repo, BuildType buildType) {
		def bDef = getResource(buildType.toString().toLowerCase(), 'CI')
		bDef.repository = repo
		def memberData = memberManagementService.getMember(collection, 'z091182')

		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: ['api-version':'4.1'],
				body: body,
				headers: [Accept: 'application/json'],
				)
	}

	public def ensureReleaseBuild(def collection, def project, def repo, BuildType buildType) {
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

	public def getResource(String buildType, String phase) {
		def s = getClass().getResourceAsStream("/build_templates/${buildType}-${phase}.json")
		JsonSlurper js = new JsonSlurper()
		return js.parse(s)
	}
}

enum BuildType {
	NONE, GRADLE, ANT, MAVEN, NODE
}
