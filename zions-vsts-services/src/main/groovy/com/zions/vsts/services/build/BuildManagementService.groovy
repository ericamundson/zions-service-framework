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

/**
 * @author z091182
 * 
 * 
 *
 */
@Component
@Slf4j
public class BuildManagementService {
	
	@Autowired
	@Value('${tfs.build.use.template:false}')
	private boolean useTfsTemplate
	
	@Autowired
	@Value('${tfs.build.generic.name:none}')
	private String genericTemplateName
	
	@Autowired
	@Value('${tfs.build.queue:Default}')
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
	
	public def ensureInitialTag(def collection, def project, def repo, def branchName) {
		// get tags and sort in reverse order to get latest tag / version
		def latestTag = getLatestTag(collection, project, repo)
		if (latestTag != null) {
			log.debug("BuildManagementService::ensureInitialTag -- Latest tag = ${latestTag.name}")
			createInitialBuildTag(collection, project, repo, latestTag, branchName)
		}
	}
	
	def ensureBuildFolder(def collection, def project, String folder) {
		def projectData = projectManagementService.getProject(collection, project, true)
		return createBuildFolder(collection, projectData, folder)
	}

	def createBuildFolder(def collection, def projectData, String folder) {
		def efolder = URLEncoder.encode(folder, 'utf-8')
		efolder = efolder.replace('+', '%20')
		log.debug("BuildManagementService::createBuildFolder -- Folder name = ${efolder}")
		
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
		log.debug("BuildManagementService::detectBuildType -- Getting top-level source files for repo ${repo.name} ...")
		def topFiles = codeManagementService.listTopLevel(collection, project, repo)
		def buildType = BuildType.NONE
		if (topFiles != null) {
			log.debug("BuildManagementService::detectBuildType -- Found some top-level files ...")
			//topFiles.value.each { file ->
			for (def file in topFiles.value) {
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
	
	public def ensureBuildsForBranch(def collection, def projectData, def repo, boolean isDRBranch, def ciTemplate, def releaseTemplate) {
		// if this is for a DR branch, call special operation
		if (isDRBranch) {
			return ensureDRBuilds(collection, projectData, repo)
		}
		def buildTemplate = null
		boolean buildFolderCreated = false
		log.debug("BuildManagementService::ensureBuildsForBranch -- Look for existing CI Build ...")
		Integer ciBldId = -1
		def ciBldName = ""
		def buildFolderName = ""
		def build = getBuild(collection, projectData, repo, 'ci')
		if (build.count == 0) {
			buildTemplate = getBuildTemplate(collection, projectData, repo, 'ci', ciTemplate)
			if (buildTemplate == null) {
				log.error("BuildManagementService::ensureBuildsForBranch -- CI build template not found.")
				//return null
			} else {
				log.debug("BuildManagementService::ensureBuildsForBranch -- Using CI build template ${buildTemplate.name}")

				// make sure build folder is available for the repo
				createBuildFolder(collection, projectData, "${repo.name}")
				buildFolderCreated = true
				buildFolderName = "${repo.name}"
				log.debug("BuildManagementService::ensureBuildsForBranch -- Build folder created for ${repo.name}")
				def ciBuild = createBuildFromTemplate(collection, projectData, repo, buildTemplate, 'ci', "${repo.name}")
				if (ciBuild == null ) {
					log.error("BuildManagementService::ensureBuildsForBranch -- CI Build creation failed!")
				} else {
					ciBldId = Integer.parseInt("${ciBuild.id}")
					ciBldName = "${ciBuild.name}"
					log.debug("BuildManagementService::ensureBuildsForBranch -- CI Build created: "+ciBldName)
				}
			}
		} else {
			log.debug("BuildManagementService::ensureBuildsForBranch -- Found existing CI Build. Setting Id for return to "+build.value[0].id)
			ciBldId = build.value[0].id
			//ciBldName = "${build.name}"
			// assume if build was found that folder is already created
			buildFolderCreated = true
		}
		Integer relBldId = -1
		def relBldName = ""
		def build1 = getBuild(collection, projectData, repo, 'release')
		if (build1.count == 0) {
			buildTemplate = getBuildTemplate(collection, projectData, repo, 'release', releaseTemplate)
			if (buildTemplate == null) {
				log.error("BuildManagementService::ensureBuildsForBranch -- Release build template not found.")
				// Not returning null here and stopping since the CI build was created so let's go ahead and create the build validation policy
				//return null
			} else {
				log.debug("BuildManagementService::ensureBuildsForBranch -- Using Release build template ${buildTemplate.name}")
				// make sure build folder is available for the repo
				if (!buildFolderCreated) {
					createBuildFolder(collection, projectData, folder)
				}
				def relBd = createBuildFromTemplate(collection, projectData, repo, buildTemplate, 'release', "${repo.name}")
				if (relBd == null ) {
					log.error("BuildManagementService::ensureBuildsForBranch -- Release Build creation failed!")
				} else {
					relBldId = Integer.parseInt("${relBd.id}")
					relBldName = "${relBd.name}"
					log.debug("BuildManagementService::ensureBuildsForBranch -- Release build created: "+relBldName)
				}
			}
		} else {
			log.debug("BuildManagementService::ensureBuildsForBranch -- Found existing Release Build for ${repo.name}.")
			//relBldId = build1.value[0].id
		}
		def returnObject = [folderName: buildFolderName,
							ciBuildId: ciBldId,
							ciBuildName: ciBldName,
							releaseBuildId: relBldId,
							releaseBuildName: relBldName]

		return returnObject
	}
	
	public def ensureDRBuilds(def collection, def projectData, def repo) {
		Integer ciBldId = -1
		def ciBldName = ""
		// set folder name as we are assuming CI and Release builds exist for the repo 
		def buildFolderName = "${repo.name}"
		log.debug("BuildManagementService::ensureDRBuilds -- Look for existing DR CI Build for ${repo.name} ...")
		def build = getDRBuild(collection, projectData, repo, 'ci')
		if (build.count == 0) {
			log.debug("BuildManagementService::ensureDRBuilds -- Existing DR CI build not found.  Get CI Build for ${repo.name} ...")
			def buildTemplate = getBuild(collection, projectData, "${repo.name}-ci")
			if (buildTemplate == null) {
				log.error("BuildManagementService::ensureDRBuilds -- CI build not found for ${repo.name}.")
			} else {
				log.debug("BuildManagementService::ensureDRBuilds -- Found CI build for ${repo.name}.  Using as template")
				def ciBuild = createDRBuildDefinition(collection, projectData, repo, buildTemplate, 'ci', "${repo.name}")
				if (ciBuild == null ) {
					log.error("BuildManagementService::ensureDRBuilds -- DR CI Build creation failed!")
				} else {
					ciBldId = Integer.parseInt("${ciBuild.id}")
					ciBldName = "${ciBuild.name}"
					log.debug("BuildManagementService::ensureDRBuilds -- DR CI Build created: "+ciBldName)
				}
			}
		} else {
			log.debug("BuildManagementService::ensureDRBuilds -- Found exsting DR CI Build for ${repo.name}. Setting Id for return to "+build.value[0].id)
			ciBldId = build.value[0].id
			//ciBldName = "${build.name}"
		}
		Integer relBldId = -1
		def relBldName = ""
		log.debug("BuildManagementService::ensureDRBuilds -- Look for existing DR Release Build for ${repo.name} ...")
		def build1 = getDRBuild(collection, projectData, repo, 'release')
		if (build1.count == 0) {
			log.debug("BuildManagementService::ensureDRBuilds -- Existing DR Release build not found.  Get Release Build for ${repo.name} ...")
			def buildTemplate = getBuild(collection, projectData, "${repo.name}-release")
			if (buildTemplate == null) {
				log.error("BuildManagementService::ensureDRBuilds -- Release build not found for ${repo.name}.")
				// Not returning null here and stopping since the CI build was created so let's go ahead and create the build validation policy
				//return null
			} else {
				def relBd = createDRBuildDefinition(collection, projectData, repo, buildTemplate, 'release', "${repo.name}")
				if (relBd == null ) {
					log.error("BuildManagementService::ensureDRBuilds -- DR Release Build creation failed!")
				} else {
					relBldId = Integer.parseInt("${relBd.id}")
					relBldName = "${relBd.name}"
					log.debug("BuildManagementService::ensureDRBuilds -- DR Release build created: "+relBldName)
				}
			}
		} else {
			log.debug("BuildManagementService::ensureDRBuilds -- Found exsting DR Release Build for ${repo.name}.")
		}
		def returnObject = [folderName: buildFolderName,
							ciBuildId: ciBldId,
							ciBuildName: ciBldName,
							releaseBuildId: relBldId,
							releaseBuildName: relBldName]

		return returnObject
	}
	
	public def getBuildTemplate(def collection, def project, def repo, String buildStage, String templateName) {
		log.debug("BuildManagementService::getBuildTemplate -- Build template specified in properties is "+templateName)
		// if build template not specified in the build properties file, try to determine build type and load the one for build type
		def buildType = BuildType.NONE
		if (templateName == null) {
			log.debug("BuildManagementService::getBuildTemplate -- Build template not specified in file; detecting build type ...")
			buildType = detectBuildType(collection, project, repo)
			log.debug("BuildManagementService::getBuildTemplate -- Detected build type = ${buildType}")
			if (buildType != BuildType.NONE) {
				// Looking for template build definition with name like 'template-maven-ci', 'template-gradle-release', 'template-ant-ci', etc.
				templateName = "template-"+buildType.toString().toLowerCase()+"-"+buildStage
			}
		}
		def bDef = null
		// if we found a valid build type, try to load the build template from ADO project first
		if (templateName != null) {
			log.debug("BuildManagementService::getBuildTemplate -- Loading ADO build template: "+templateName)
			// first try to load the template from ADO project templates
			bDef = getTemplate(collection, project, templateName)
			if (bDef == null) {
				// no ADO project template found so load from resource file
				log.debug("BuildManagementService::getBuildTemplate -- Using local resource file.  File name: "+ templateName)
				bDef = getResource(buildType.toString().toLowerCase(), buildStage, templateName)
				if (bDef == null) {
					// no resource found matching template name so try to load generic ADO project template
					log.debug("BuildManagementService::getBuildTemplate -- Build template "+templateName+" not found. Loading generic template from ADO project ...")
					bDef = getTemplate(collection, project, "template-"+this.genericTemplateName+"-"+buildStage)
					if (bDef == null) {
						// no ADO project generic template found so load from generic resource
						log.debug("BuildManagementService::getBuildTemplate -- ADO generic template "+templateName+" not found. Loading generic template template-"+this.genericTemplateName+"-"+buildStage + " from resources ...")
						bDef = getResource(buildType.toString().toLowerCase(), buildStage, "template-"+this.genericTemplateName+"-"+buildStage)
						if (bDef == null) {
							log.debug("BuildManagementService::getBuildTemplate -- No usable build definition template was found. No build will be created.")
						} else {
							// indicate we're using a resource file
							this.useTfsTemplate = false
						}
					}
				} else {
					// indicate we're using a resource file
					this.useTfsTemplate = false
				}
			}
		}
		return bDef
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

	// intended as an alternative to calling createBuild to prevent from looking up and loading the build template a second time
	public def createBuildFromTemplate(def collection, def project, def repo, def buildTemplate, String buildStage, def folder) {
		return createBuildDefinition(collection, project, repo, buildTemplate, buildStage, folder);
	}

	public def createBuild(def collection, def project, def repo, BuildType buildType, String buildStage, def folder) {
		def bDef = null
		if (this.useTfsTemplate) {
			// Looking for template build definition with name like 'template-maven-ci', 'template-gradle-release', 'template-ant-ci', etc.
			String templateName = "template-"+buildStage+"-"+buildType.toString().toLowerCase()
			log.debug("BuildManagementService::createBuild -- Using TFS template: "+templateName)
			//bDef = getBuild(collection, project, templateName)
			bDef = getTemplate(collection, project, templateName)
			if (bDef == null) {
				log.debug("BuildManagementService::createBuild -- Build template for "+buildType.toString().toLowerCase()+", build stage: "+ buildStage+" not found. Using generic template.")
				bDef = getTemplate(collection, project, "template-"+buildStage+"-"+this.genericTemplateName)
			}
		} else {
			log.debug("BuildManagementService::createBuild -- Using local resource file.  Build type: "+buildType.toString().toLowerCase()+", build stage: "+ buildStage)
			bDef = getResource(buildType.toString().toLowerCase(), buildStage)
		}
		if (bDef == null) {
			log.debug("BuildManagementService::createBuild -- No usable build definition template was found. No build will be created.")
			return null
		}
		return createBuildDefinition(collection, project, repo, bDef, buildStage, folder);
	}
	
	def createBuildDefinition(def collection, def project, def repo, def bDef, String buildStage, def folder) {
		// set all the necessary properties and post the request
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
//		bDef.demands = []
//		bDef.variableGroups = []
		bDef.properties = [source: 'AllDefinitions']
//		bDef.quality = 1
//		bDef.queueStatus = 0
//		bDef.type = 2
//		bDef.jobAuthorizationScope = 1
//		bDef.triggers = []
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
		// if using a resource (json) file as the template ensure the correct agent queue 
		if (!this.useTfsTemplate) {
			def queueData = getQueue(collection, project, "${this.queue}")
			log.debug("BuildManagementService::createBuildDefinition - Queue = ${queueData}")
			if (queueData != null) {
				bDef.queue = queueData
				bDef.process.phases.each { phase ->
					phase.target.queue = queueData
				}
			}
		}
		//def memberData = memberManagementService.getMember(collection, 'z091182')
		return writeBuildDefinition(collection, project, bDef)
	}

	def createDRBuildDefinition(def collection, def project, def repo, def bDef, String buildStage, def folder) {
		// set all the necessary properties and post the request
		bDef.remove('authoredBy')
		bDef.name = "${repo.name}-dr-${buildStage}"
		bDef.id = -1
		bDef.draftOf = null
		bDef.path = "${folder}"
		bDef.counters = [:]
		bDef.comment = "DR ${buildStage} build for ${repo.name}"
		bDef.createdDate = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		bDef.project = project
		bDef.badgeEnabled = false
		//bDef.demands = []
		//bDef.variableGroups = []
		bDef.properties = [source: 'AllDefinitions']
		if ("${buildStage}".equalsIgnoreCase("release")) {
			def branchFilters = ["+refs/heads/DR/*"]
			def drTrigger = ["branchFilters": branchFilters, "pathFilters": [], "batchChanges": false, "maxConcurrentBuildsPerBranch": 1, "pollingInterval": 0, "triggerType": "continuousIntegration"]
			bDef.triggers = [drTrigger]
		}
		bDef.project = project
		bDef.repository.id = "${repo.id}"
		bDef.repository.name = "${repo.name}"
		bDef.repository.url = "${repo.url}"
		bDef.repository.defaultBranch = "${repo.defaultBranch}"
		bDef.retentionSettings = getRetentionSettings(collection)
		def queueData = getQueue(collection, project, "On-Prem DR")
		if (queueData != null) {
			bDef.queue = queueData
			bDef.process.phases.each { phase ->
				phase.target.queue = queueData
			}
		}
		return writeBuildDefinition(collection, project, bDef)
	}

	def writeBuildDefinition(def collection, def project, def bDef) {
		def body = new JsonBuilder(bDef).toPrettyString()
		//log.debug("BuildManagementService::writeBuildDefinition --> ${body}")
		
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
		return result
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

	public def getDRBuild(def collection, def project, def repo, def qualifier) {
		log.debug("BuildManagementService::getDRBuild -- buildName = "+repo.name+"-dr-"+qualifier)
		def query = ['api-version':'4.1','name':"${repo.name}-dr-${qualifier}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
		return result
	}

	public def getTemplate(def collection, def project, def name) {
		log.debug("BuildManagementService::getTemplate -- templateName = "+name)
		def query = ['api-version':'4.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/templates",
				query: query,
				)
		if (result.value != null) {
			def bt = result.value.find { buildTemplate ->
				"${buildTemplate.name}" == "${name}" 
			}
			if (bt != null) {
				return bt.template
			}
		}
		return null
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

	public def getBuildById(def collection, def project, def id) {
		log.debug("BuildManagementService::getBuildById -- ID = " + id)
		def query = ['api-version':'4.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${id}",
				query: query,
				)
		if (result == null) {
			log.debug("BuildManagementService::getBuildById -- build with ID " + id + " not found. Returning NULL ...")
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
			log.debug("BuildManagementService::getResource -- Exception caught reading resource with name ${filename}.json not found. Returning NULL ...")
		}
		return template
	}

	private def getLatestTag(def collection, def project, def repo) {
		log.debug("BuildManagementService::getLatestTag for project ${project.name} and repo ${repo.name}")
		def query = ['filter':'tags', 'api-version':'4.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/refs",
			query: query,
		)
		if (result.count == 0) {
			log.debug("BuildManagementService::getLatestTag -- No tags found for project ${project.name} and repo ${repo.name}")
			return null
		}
		def lastTagIdx = result.count - 1
		return result.value[lastTagIdx]
	
		// parse tag name
		/*
		def outtags = [];
		for (def tag in result.value) {		
			def tagName = tag.name;
			tagName = tagName.substring("refs/tags/".length(), tagName.length());
			outtags.push(tagName);
		}
	
		// find the latest tag
		def latestTag  = tagName
		
		return latestTag
		*/
	}

	private def createInitialBuildTag(def collection, def project, def repo, def latestTag, def branchName) {
		String versionTag = latestTag.name.substring("refs/tags/".length())
		log.debug("BuildManagementService::createInitialBuildTag  -- versionTag = ${versionTag}")
		def parts = versionTag.tokenize(".")
		def version = parts[0] + '.' + parts[1] + '.' + parts[2]
		log.debug("BuildManagementService::createInitialBuildTag  -- Version = ${version}")
		// use IFB branch name as qualifier
		def begIdx = -1
		if (branchName.toLowerCase().startsWith("refs/heads/ifb/")) {
			begIdx = "refs/heads/ifb/".length()
		} else {
			begIdx = "refs/heads/feature/ifb/".length()
		}
		def qualifier = branchName.substring(begIdx)
		log.debug("BuildManagementService::createInitialBuildTag  -- qualifier = ${qualifier}")
		def nTag = version + "-" + qualifier + "-00000";
		log.debug("BuildManagementService::createInitialBuildTag  -- New Tag = ${nTag}")
		// get the commit to which to apply the tag
		def query = ['api-version':'5.0-preview.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/annotatedtags/${latestTag.objectId}",
				query: query,
				)
		def commitId = result.objectId
		log.debug("BuildManagementService::createInitialBuildTag -- Latest tag commit SHA is ${commitId}")

		log.debug("BuildManagementService::createInitialBuildTag -- Creating initial build tag ${nTag} ...")
		def resp = "OK"
		//def gitObject = [ "objectId": "${commitId}" ];
		def newTag = ["name": "${nTag}", "message": "initial tag", "taggedObject": ["objectId": "${commitId}"]];
		def body = new JsonBuilder(newTag).toPrettyString()
		def tRes = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/annotatedtags",
			body: body,
			headers: [Accept: 'application/json;api-version=5.0-preview.1']
		)
		if (tRes == null) {
			log.error("BuildManagementService::createInitialBuildTag -- Unable to create new Git tag.")
			resp == "FAILED"
		}
		return resp;
	}
}

enum BuildType {
	NONE, GRADLE, ANT, MAVEN, NODE
}
