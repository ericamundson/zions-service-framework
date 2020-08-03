package com.zions.vsts.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j;
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseException

@Component
@Slf4j
class CodeManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired 
	private ProjectManagementService projectManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService
	
	@Autowired
	private MemberManagementService memberManagementService
	
	@Autowired
	private PermissionsManagementService permissionsManagementService
	
	@Autowired
	private CommandManagementService commandManagementService

	public CodeManagementService() {
		
	}
	
	def getCommits(String url) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: ['api-version': '5.1']
			)
	}
	
	def getChanges(String url) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: ['api-version': '5.1']
			)
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
	
	public def getRepos(String collection, def project, def team) {
		def teamData = memberManagementService.getTeam(collection, project, team)
		def repos = []
		def query = ['api-version':'4.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories",
			query: query
		)
		
		result.value.each { repo -> 
			if (permissionsManagementService.hasIdentity(collection, project, repo, teamData)) {
				repos.add(repo)
			}
		}
		return repos

	}

	public def listTopLevel(def collection, def project, def repo) {
		//log.debug("CodeManagementService::listTopLevel -- collection: ${collection}, project: ${project.id}, repo: ${repo.id}")
		def query = ['api-version':'5.1','scopePath':'/', 'recursionLevel':'OneLevel', latestProcessedChange:true, 'versionDescriptor.version':'master','versionDescriptor.versionOptions':'firstParent']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/items",
			query: query
		)
		//log.debug("CodeManagementService::listTopLevel -- Return result: "+result)
		return result

	}
	
	public def getBranches(String collection, String project, String repo) {
		def query = ['api-version':'5.1', 'baseVersionDescriptor.versionType': 'branch']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/git/repositories/${repo}/stats/branches",
			query: query
		)
		return result
	}

	public def getBranch(String collection, String project, String repo, String name) {
		def query = ['api-version':'5.1', 'baseVersionDescriptor.versionType': 'branch']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/git/repositories/${repo}/stats/branches",
			query: query
		)
		def branch = result.'value'.find { b -> 
			String bName = "${b.name}"
			bName == name
		}
		
		return branch
	}

	public def ensureBranch(String collection, String project, String repo, String baseName, String name) {
		def nBranch = getBranch(collection, project, repo, name)
		if (nBranch) {
			return nBranch
		} else {
			def bBranch = getBranch(collection, project, repo, baseName)
			nBranch = createBranch(collection, project, repo, bBranch, name)
			nBranch = getBranch(collection, project, repo, name)
		}
		return nBranch
	}
	
	public def createBranch(String collection, String project, String repo, def bBranch, String name) {
		def query = ['api-version': '5.1']
		def data = [[name: "refs/heads/${name}", newObjectId: "${bBranch.commit.commitId}", oldObjectId: "0000000000000000000000000000000000000000"]]
		String body = new JsonBuilder(data).toPrettyString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			body: body,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/git/repositories/${repo}/refs",
			query: query
		)
		if (result && result.'value' && result.'value'.size() == 1) {
			return result.'value'[0]
		}
		return null
	}


	public def getFileContent(def collection, def project, def repo, def filename, def branchName) {
		log.debug("CodeManagementService::getFileContent -- collection: ${collection}, project: ${project.name}, repo: ${repo.name}, filename: ${filename}, branchName: ${branchName}")
		String filePath = "/${filename}"
		def query = ['api-version':'5.1','path':filePath, 'includeContent':true, 'versionDescriptor.version':"${branchName}",'versionDescriptor.versionType':'branch']
		def result
		try {
			result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.name}/_apis/git/repositories/${repo.name}/items",
				query: query
			)
			log.debug("CodeManagementService::getFileContent -- Return result: "+result)
		} catch (HttpResponseException hre) {
			// check for Not Found
			if (hre.getStatusCode() == 404) {
				log.debug("CodeManagementService::getFileContent -- File ${filename} not found")
				result = null
			}
		}
		if (result == null) return null
		return result.content
	}

	public def importRepo(String collection, String project, String repoName, String importUrl, String bbUser, String bbPassword) {
		def projectData = projectManagementService.getProject(collection, project)
		def repo = ensureRepo(collection, projectData, repoName)
		def endpoint = endpointManagementService.createGITServiceEndpoint(collection, projectData.id, importUrl, bbUser, bbPassword)
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
	
	def importRepoDir(String collection, String project, String repoName, File repoDir, String inUser, String inPassword) {
		def projectData = projectManagementService.getProject(collection, project)
		def repo = ensureRepo(collection, projectData, repoName)
//		File gitDir = new File(repoDir, '.git')
//		if (!gitDir.exists()) {
//			
//		}
		
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def erepoName = URLEncoder.encode(repoName, 'utf-8')
		erepoName = erepoName.replace('+', '%20')
		def turl = getAuthUrl("${genericRestClient.tfsUrl}/${eproject}/_git/${erepoName}", genericRestClient.user, genericRestClient.token)
		commandManagementService.executeCommand("git remote set-url origin ${turl}", repoDir)
		commandManagementService.executeCommand("git push", repoDir)
	}
	
	def importRepoCLI(String collection, String project, String repoName, String importUrl, String inUser, String inPassword) {
		def projectData = projectManagementService.getProject(collection, project)
		def repo = ensureRepo(collection, projectData, repoName)
		def ourl = getAuthUrl(importUrl, inUser, inPassword)
		File gitDir = new File('git')
		
		if (!gitDir.exists())
		{
			gitDir.mkdir()
		}
		
		commandManagementService.executeCommand("git clone ${ourl}", gitDir)
		File repoDir = new File(gitDir, repoName )
		
		commandManagementService.executeCommand("git fetch", repoDir)
		commandManagementService.executeCommand("git pull", repoDir)

		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def erepoName = URLEncoder.encode(repoName, 'utf-8')
		erepoName = erepoName.replace('+', '%20')
		def turl = getAuthUrl("${genericRestClient.tfsUrl}/${eproject}/_git/${erepoName}", genericRestClient.user, genericRestClient.token)
		commandManagementService.executeCommand("git remote set-url origin ${turl}", repoDir)
		commandManagementService.executeCommand("git push", repoDir)
	}
	
	def getAuthUrl(def url, def userid, def password) {
		String encodedPassword = URLEncoder.encode(password, 'utf-8')
		url = "https://" + userid + ":" + encodedPassword + "@"+ url.substring("https://".length())
	}
	
	
	public def ensureDeployManifest(def collection, def project, def repo) {
		def manifest = getDeployManifest(collection, project, repo)
		if (manifest == null) {
			manifest = createDeployManifest(collection, project, repo)
		}
	}
	
	public def ensureFile(def collection, def project, def repo,  def filepath, String fileContent) {
		def content = getFileContent(collection, project, repo, filepath, 'master')
		if (!content) {
			content = createFile(collection, project, repo, filepath, fileContent)
		} else {
			content = updateFile(collection, project, repo, filepath, fileContent)
		}
		return content
	}

	public def ensureGitAttributes(def collection, def project, def repo) {
		def gitAttriibutes = getGitAttributes(collection, project, repo)
		if (gitAttriibutes == null) {
			gitAttriibutes = createGitAttributes(collection, project, repo)
		}
	}
	
	def getRefHead(def collection, def project, def repo) {
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
	
	def getBranches(def collection, def project, def repo) {
		def query = [filter:'heads/','api-version':'5.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/refs",
			query: query,
			)
		return result
	}

	def createDeployManifest(def collection, def project, def repo) {
		def query = [filter:'head', 'api-version': '4.1']
		def head = getRefHead(collection, project, repo)
		if (head == null) return null
		def mContent = '''<?xml version="1.0" encoding="utf-8"?>
<udm.DeploymentPackage application="@@PROJECT@@/@@REPO@@" version="@Tag@">
  <deployables />
</udm.DeploymentPackage>'''
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
	def createFile(def collection, def project, def repo, String filepath, String outC) {
		def query = [filter:'head', 'api-version': '4.1']
		def head = getRefHead(collection, project, repo)
//		if (head == null) return null
		def fileData = [commits: [[changes:[[changeType: 'add', item:[path:"${filepath}"], newContent: [content: outC, contentType:0]]], comment: "Added ${filepath}"]],
			refUpdates:[[name:'refs/heads/master', oldObjectId: '0000000000000000000000000000000000000000']]]
		if (head != null) {
			fileData = [commits: [[changes:[[changeType: 'add', item:[path:"${filepath}"], newContent: [content: outC, contentType:0]]], comment: "Added/Updated ${filepath}"]],
				refUpdates:[[name:'refs/heads/master', oldObjectId: head.objectId]]]
		}
		def body = new JsonBuilder(fileData).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/git/repositories/${repo.id}/pushes",
			query: query,
			body: body
			)
//	    def dItems = []
//		String[] fileItems = filepath.split('/')
//		String sOut = '/'
//		fileItems.each { String item ->
//			String iOut = "${sOut}"
//			dItems.add([path:iOut,version: 'master', versionType: 'branch', versionLevel:4])
//			sOut = "${sOut}/${item}"
//			
//		}
//		String iOut = "${sOut}"
//		dItems.add([path:iOut,version: 'master', versionType: 'branch', versionLevel:4])
//		def items = [includeContentMetadata: true, itemDescriptors:[dItems]]
//		body = new JsonBuilder(items).toString()
//		result = genericRestClient.post(
//			contentType: ContentType.JSON,
//			requestContentType: 'application/json',
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/itemsBatch",
//			query: query,
//			body: body
//			)
	}
	
	def updateFile(def collection, def project, def repo, String filepath, String outC) {
		def query = [filter:'head', 'api-version': '4.1']
		def head = getRefHead(collection, project, repo)
		def fileData = [commits: [[changes:[[changeType: 'edit', item:[path:"${filepath}"], newContent: [content: outC, contentType:0]]], comment: "Added ${filepath}"]],
			refUpdates:[[name:'refs/heads/master', oldObjectId: '0000000000000000000000000000000000000000']]]
		if (head != null) {
			fileData = [commits: [[changes:[[changeType: 'edit', item:[path:"${filepath}"], newContent: [content: outC, contentType:0]]], comment: "Added/Updated ${filepath}"]],
				refUpdates:[[name:'refs/heads/master', oldObjectId: head.objectId]]]
		}
		def body = new JsonBuilder(fileData).toString()
		//println body
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/git/repositories/${repo.id}/pushes",
			query: query,
			body: body
			)
		return outC
//	    def dItems = []
//		String[] fileItems = filepath.split('/')
//		String sOut = '/'
//		fileItems.each { String item ->
//			String iOut = "${sOut}"
//			dItems.add([path:iOut,version: 'master', versionType: 'branch', versionLevel:4])
//			sOut = "${sOut}/${item}"
//
//		}
//		String iOut = "${sOut}"
//		dItems.add([path:iOut,version: 'master', versionType: 'branch', versionLevel:4])
//		def items = [includeContentMetadata: true, itemDescriptors:[dItems]]
//		body = new JsonBuilder(items).toString()
//		result = genericRestClient.post(
//			contentType: ContentType.JSON,
//			requestContentType: 'application/json',
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/itemsBatch",
//			query: query,
//			body: body
//			)
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

	def createGitAttributes(def collection, def project, def repo) {
		log.debug("CodeManagementService::createGitAttributes -- Insert .gitattributes file for ${repo.name} repo in ${project.name} project")
		def query = [filter:'head', 'api-version': '5.1']
		def head = getRefHead(collection, project, repo)
		if (head == null) {
			log.debug("CodeManagementService::createGitAttributes -- Unable to get head ref for ${repo.name} repo in ${project.name} project")
			return null
		}
		def inContent = getResource("gitattributes.txt")
		log.debug("CodeManagementService::createGitAttributes -- Read resource for .gitattributes file: ${inContent}")
		def outContent = "${inContent}"
		def commitData = [commits: [[changes:[[changeType: 'add', item:[path:'/.gitattributes'], newContent: [content: outContent, contentType:'rawText']]], comment: 'Add .gitattributes file']],
			refUpdates:[[name:'refs/heads/master', oldObjectId: head.objectId]]]
		def body = new JsonBuilder(commitData).toString()
		log.debug("CodeManagementService::createGitAttributes -- Commit data (json): ${body}")
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/git/repositories/${repo.id}/pushes",
			query: query,
			body: body
			)
			
		def items = [includeContentMetadata: true, itemDescriptors:[[path: '/.gitattributes', version: 'master', versionType: 'branch', versionLevel: 4],
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
	
	def getGitAttributes(def collection, def project, def repo) {
		def query = ['api-version':'5.1','scopePath':'/', 'recursionLevel':'OneLevel', latestProcessedChange:true, 'versionDescriptor.version': 'master']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/items",
			query: query,
			)
		def gitAttributes = null
		if (result != null) {
			result.value.each { file ->
				if ("${file.path}" == '/.gitattributes') {
					gitAttributes = file
				}
			}
		}
		return gitAttributes
	}

	public def getResource(String resourcePath) {
		def resrc = null
		try {
			//resrc = getClass().getResourceAsStream("/${resourcePath}")
			resrc = getClass().getResource("/${resourcePath}").text
			//String xsd = this.getClass().getResource('/xsd/file.xsd').text
		} catch (e) {
			log.debug("CodeManagementService::getResource -- Exception caught reading resource ... ${resourcePath} not found. Returning NULL ...")
		}
		return resrc
	}
}
