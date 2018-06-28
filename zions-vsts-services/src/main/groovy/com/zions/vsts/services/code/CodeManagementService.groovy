package com.zions.vsts.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j;
import groovyx.net.http.ContentType

@Component
@Slf4j
class CodeManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	@Autowired 
	private ProjectManagementService projectManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService

	public CodeManagementService() {
		
	}
	
	public def ensureRepo(String collection, def project, String repoName) {
		def repo = getRepo(collection, project, repoName)
		if (repo == null) {
			repo = createRepo(collection, project, repoName)
		}
		return repo
	}
	
	public def createRepo(String collection, def project, String repoName) {
		def query = ['api-version':'4.1']
		def reqObj = [name: repoName, project: [id: project.id, name: project.name]]
		def body = new JsonBuilder(reqObj).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories",
			query: query,
			body: body
			)
		return result

	}
	
	public def getRepo(String collection, def project, String repoName) {
		def query = ['api-version':'4.1']
		def repoNameE = URLEncoder.encode(repoName, 'UTF-8')
		repoNameE= repoNameE.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repoNameE}",
			query: query,
			)
		return result

	}
	public def getRepos(String collection, def project) {
		def query = ['api-version':'4.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories",
			query: query
		)
		return result

	}

	public def listTopLevel(def collection, def project, def repo) {
		//log.debug("CodeManagementService::listTopLevel -- collection: ${collection}, project: ${project.id}, repo: ${repo.id}")
		def query = ['api-version':'4.0','scopePath':'/', 'recursionLevel':'OneLevel', latestProcessedChange:true, 'versionDescriptor.version':'master','versionDescriptor.versionOptions':'firstParent']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/items",
			query: query
		)
		//log.debug("CodeManagementService::listTopLevel -- Return result: "+result)
		return result

	}


	public def importRepo(String collection, String project, String repoName, String importUrl, String bbUser, String bbPassword) {
		def projectData = projectManagementService.getProject(collection, project)
		def repo = ensureRepo(collection, projectData, repoName)
		def endpoint = endpointManagementService.createServiceEndpoint(collection, projectData.id, importUrl, bbUser, bbPassword)
		def query = ['api-version':'4.1-preview.1']
		def reqObj = [parameters: [deleteServiceEndpointAfterImportIsDone: true, gitSource: [url: importUrl, overwrite: false], serviceEndpointId: endpoint.id, tfvcSource: null]]
		def body = new JsonBuilder(reqObj).toString()
		def repoNameE = URLEncoder.encode(repoName, 'UTF-8')
		repoNameE = repoNameE.replace('+', '%20')
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/git/repositories/${repoNameE}/importRequests",
			query: query,
			body: body
		)
		//ensureDeployManifest(collection, projectData, repo)
		return result

	}
	
	public def ensureDeployManifest(def collection, def project, def repo) {
		def manifest = getDeployManifest(collection, project, repo)
		if (manifest == null) {
			manifest = createDeployManifest(collection, project, repo)
		}
	}
	
	def getRefHead(collection, project, repo) {
		def query = [filter:'head','api-version':'4.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/refs",
			query: query,
			)
		def head = null
		result.value.each { ref ->
			if ("${ref.name}" == 'refs/heads/master') {
				head = ref
			}
		}
		return head

	}
	
	def createDeployManifest(collection, project, repo) {
		def query = [filter:'head', 'api-version': '4.1']
		def head = getRefHead(collection, project, repo)
		if (head == null) return null
		def mContent = '''<?xml version="1.0" encoding="utf-8"?>
<udm.DeploymentPackage application="@@PROJECT@@/@@REPO@@" version="@Tag@">
  <deployables />
</udm.DeploymentPackage>"'''
		def outC = mContent.replace('@@PROJECT@@', project.name)
		outC = outC.replace('@@REPO@@', repo.name)
		def manifestData = [commits: [[changes:[[changeType: 1, item:[path:'/dar/deployit-manifest.xml'], newContent: [content: outC, contentType:0]]], comment: 'Added deployit-manifest.xml']],
			refUpdates:[[name:'refs/heads/master', oldObjectId: head.objectId]]]
		def body = new JsonBuilder(manifestData).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/git/repositories/${repo.id}/pushes",
			query: query,
			body: body
			)
			
		def items = [includeContentMetadata: true, itemDescriptors:[[path: '/dar/deployit-manifest.xml', version: 'master', versionType: 'branch', versionLevel: 4],
			[path: '/dar', version: 'master', versionType: 'branch', versionLevel: 4],
			[path: '/', version: 'master', versionType: 'branch', versionLevel: 4]]]
		body = new JsonBuilder(items).toString()
		result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/itemsBatch",
			query: query,
			body: body
			)
	}
	
	def getDeployManifest(def collection, def project, def repo) {
		def query = ['api-version':'4.1','scopePath':'/dar/', 'recursionLevel':'OneLevel', latestProcessedChange:true, 'versionDescriptor.version': 'master']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/items",
			query: query,
			)
		def manifest = null
		if (result != null) {
			result.value.each { file ->
				if ("${file.path}" == '/dar/deployit-manifest.xml') {
					manifest = file
				}
			}
		}
		return manifest
	}
}
