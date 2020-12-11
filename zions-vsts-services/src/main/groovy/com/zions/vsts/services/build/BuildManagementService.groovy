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
	@Value('${tfs.build.yamlFilenames:}')
	private String yamlFilenamesStr

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
		
		def query = ['api-version':'5.0-preview.1','excludeUrls':true]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/build/folders/${efolder}",
			query: query
		)
		if (result == null) {
			def folderObj = [description: '', path: "\\${folder}"]
			
			def body = new JsonBuilder(folderObj).toPrettyString()
			result = genericRestClient.put(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/build/folders/${efolder}",
				body: body,
				headers: [Accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'],
			)
		}
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
	
	public def ensureBuildsForBranch(def collection, def projectData, def repo, boolean isDRBranch, def ciTemplate, def releaseTemplate, boolean isInitBranch, def buildData = null) {
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
		Integer relBldId = -1
		def relBldName = ""
		// check for YAML pipeline in use
		boolean isYAMLPipeline = false
		def pipelineFileName = null
		if (buildData && buildData.ciBuildFile) {
			log.debug("BuildManagementService::ensureBuildsForBranch -- Setting YAML CI Build filename to ${buildData.ciBuildFile}")
			pipelineFileName = "${buildData.ciBuildFile}"
			isYAMLPipeline = true
		}
		def branchName = "master"
		if (isInitBranch) {
			branchName = "adoinit"
		}
		if (pipelineFileName == null) {
			pipelineFileName = getYamlBuildFilename(collection, repo, branchName)
			if (pipelineFileName) {
				// indicate the repo/branch is using a YAML pipeline
				isYAMLPipeline = true
			}
		}
		if (isYAMLPipeline) {
			// look for default pipeline build def to use for build validation policy
			String ciBuildInt = "${repo.name} CI"
			if (buildData && buildData.ciBuildName) {
				ciBuildInt = buildData.ciBuildName
			}
			def yamlBuild = getBuild(collection, projectData, ciBuildInt)
			if (yamlBuild != null) {
				log.debug("BuildManagementService::ensureBuildsForBranch -- Found existing YAML CI Build. Setting Id for return to ${yamlBuild.id}")
				ciBldId = Integer.parseInt("${yamlBuild.id}")
			} else {
				buildTemplate = getYAMLTemplate()
				// make sure build folder is available for the repo
				createBuildFolder(collection, projectData, "${repo.name}")
				buildFolderCreated = true
				buildFolderName = "${repo.name}"
				log.debug("BuildManagementService::ensureBuildsForBranch -- Build folder created for ${repo.name}")
				def ciBuild = createYAMLBuildDef(collection, projectData, repo, buildTemplate, "${repo.name}", ciBuildInt, pipelineFileName)
				if (ciBuild == null ) {
					log.error("BuildManagementService::ensureBuildsForBranch -- YAML CI Build creation failed!")
				} else {
					ciBldId = Integer.parseInt("${ciBuild.id}")
					ciBldName = "${ciBuild.name}"
					log.debug("BuildManagementService::ensureBuildsForBranch -- YAML CI Build created: "+ciBldName)
				}
			}
		} else {
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

	def getYAMLTemplate() {
		def bDef = null
		log.debug("BuildManagementService::getYAMLTemplate -- Calling getResource with name template-yaml ...")
		bDef = getResource("YAML", "CI", "template-yaml")
		log.debug("BuildManagementService::getYAMLTemplate -- Returning template " + bDef)
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
		return updateBuildDefinition(collection, projectData, build)
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

	def createYAMLBuildDef(def collection, def project, def repo, def bDef, def folder, String name = null, def yamlFile = null) {
		log.debug("BuildManagementService::createYAMLBuildDef for project ${project.name} / repo ${repo.name} with name "+name)
		// set all the necessary properties and post the request
		bDef.remove('authoredBy')
		bDef.name = "${repo.name} CI"
		if (name) {
			bDef.name = name
		}
		if (yamlFile) {
			bDef.process.yamlFilename = "/${yamlFile}"
		}
		bDef.id = -1
		//bDef.draftOf = null
		bDef.path = "${folder}"
		bDef.counters = [:]
		bDef.comment = "CI validation build for ${repo.name}"
		bDef.createdDate = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		bDef.project = project
		bDef.badgeEnabled = false
		//bDef.properties = [source: 'AllDefinitions']
	
		bDef.repository.id = "${repo.id}"
		bDef.repository.name = "${repo.name}"
		bDef.repository.url = "${repo.url}"
		bDef.repository.defaultBranch = "${repo.defaultBranch}"
		//bDef.retentionSettings = getRetentionSettings(collection)
		// using a resource (json) file as the template, ensure the correct agent queue 
		log.debug("BuildManagementService::createYAMLBuildDef - Getting queue name ...")
		def queueData = getQueue(collection, project, "${this.queue}")
		log.debug("BuildManagementService::createYAMLBuildDef - Queue = ${queueData}")
		if (queueData != null) {
			bDef.queue = queueData
			bDef.process.phases.each { phase ->
				phase.target.queue = queueData
			}
		}
		log.debug("BuildManagementService::createYAMLBuildDef - Writing YAML build def ...")
		return writeBuildDefinition(collection, project, bDef)
	}

	def writeBuildDefinition(def collection, def project, def bDef, def query = null) {
		def body = new JsonBuilder(bDef).toPrettyString()
		log.debug("BuildManagementService::writeBuildDefinition --> ${body}")
		
//		File f = new File("${repo.name}-${buildStage}.json")
//		def o = f.newDataOutputStream()
//		o << body
//		o.close()
		def result = null
		if (query) {
			result = genericRestClient.post(
					requestContentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
					body: body,
					query: query
					//headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true'],
					)
		} else {
			result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				body: body,
				headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true'],
				)

		}
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
		def query = ['api-version':'5.1','name':"${repo.name}-${qualifier}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
		return result
	}
	
	public def tagBuild(def build, String tag) {
		
		String url = "${build.url}/tags/${tag}"
		def result = genericRestClient.put(
			requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version': '5.1']
			)
		return result
	}
	
	public def deleteTag(def build, String tag) {
		String etag = URLEncoder.encode(tag, 'utf-8').replace('+', '%20')
		String url = "${build.url}/tags/${etag}"
		def result = genericRestClient.delete(
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version': '5.1']
			)
		return result
	}

	public def getRelatedBuilds(def collection, def project, def build, boolean isProdBranch = false, String buildTagFilter = 'none') {
		//log.debug("BuildManagementService::getBuild -- buildName = "+repo.name+"-"+qualifier)
		if (isProdBranch) {
			def tag = tagBuild(build, 'PR')
		} else if (buildTagFilter != 'none') {
			
		}
		
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		//def builds = []
		//int s = 'refs/heads/'.length()
		String bName = "${build.sourceBranch}"
		String repoId = "${build.repository.id}"
		String defId = "${build.definition.id}"
		def query = ['api-version':'5.1','branchName': bName, definitions: defId ]
//		if (isProdBranch) {
//			query.tagFilters = 'PR'
//		} else if (buildTagFilter != 'none') {
//			query.tagFilters = buildTagFilter
//		}
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/builds",
				query: query,
				)
		def builds = []
		if (result) {
			builds.addAll(result.'value')
		}
		return builds
	}

	public def getDRBuild(def collection, def project, def repo, def qualifier) {
		log.debug("BuildManagementService::getDRBuild -- buildName = "+repo.name+"-dr-"+qualifier)
		def query = ['api-version':'5.1','name':"${repo.name}-dr-${qualifier}"]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
				query: query,
				)
		return result
	}
	
	public def getExecution(String collection, String project, String buildId) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['api-version':'5.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/builds/${buildId}",
				query: query,
				)

	}

	public def updateExecution(String collection, String project, String buildId, def data) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['api-version':'5.1']
		String body = new JsonBuilder(data).toPrettyString()
		def result = genericRestClient.patch(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/builds/${buildId}",
				body: body,
				query: query,
				)
		return result
	}

	public def getExecutionWorkItems(String collection, String project, String buildId) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['api-version':'5.1']
		def wis = []
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/builds/${buildId}/workitems",
				query: query,
				)
		if (result) {
			result.'value'.each { build ->
				wis.add(build)
			}
		}
		return wis
	}
	
	public def getExecutionWorkItemsByBuilds(String collection, String project, def builds) {
		def wis = []
		builds.each { build -> 
			String bid = "${build.id}"
			def owis = getExecutionWorkItems(collection, project, bid)
			def data = [build: build, workitems: owis]
			wis.addAll(data)
		}
//		def swis = wis.toSet()
		return wis
	}
	
	public def getTags(String collection, String project, String prefix = null) {
		def tags = []
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['api-version':'5.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/tags",
				query: query,
				)
				
		if (result) {
			result.each { tag ->
				String stag = "${tag}"
				if (!prefix || (prefix && stag.startsWith(prefix))) {
					tags.add(stag)
				}
			}
		}
		return tags
	}
	
	public def getBuildTags(def build, String prefix = null) {
		def tags = []
		def query = ['api-version':'5.1']
		String buildUrl = "${build.url}"
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${buildUrl}/tags",
				query: query,
				)
				
		if (result) {
			result.'value'.each { tag ->
				String stag = "${tag}"
				if (!prefix || (prefix && stag.startsWith(prefix))) {
					tags.add(stag)
				}
			}
		}
		return tags
	}


	public def getExecutionChanges(String collection, String project, String buildId, boolean includeSourceChange = false) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['api-version':'5.1', includeSourceChange: includeSourceChange]
		def changes = []
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/build/builds/${buildId}/changes",
				query: query,
				)
		if (result) {
			result.'value'.each { change ->
				changes.add(change)
			}
		}
		return changes
	}
	
	public def getExecutionChangesByBuilds(String collection, String project, def builds, boolean includeSourceChange = false) {
		def changes = []
		builds.each { build ->
			String bid = "${build.id}"
			def ochanges = getExecutionChanges(collection, project, bid, includeSourceChange)
			changes.addAll(ochanges)
		}
		return changes
	}
	
	public def getExecutionResource(String url) {
		def query = ['api-version':'5.1']
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
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
		// What is this doing ?? query = ['api-version':'4.1', 'propertyFilters':'processParameters']
		return result1
	}

	public def updateBuilds(def collection, def project, boolean deleteUnwantedTasks) {
		def buildDefs = getBuilds(collection, project)
		buildDefs.value.each { buildDef ->
			// HACK ALERT! Should try to pass this in as a parameter
			// only process release builds and skip d3, d3z and zbc builds
			if (buildDef.name.endsWith("-release") && (!buildDef.name.startsWith("d3") && !buildDef.name.startsWith("zbc"))) {
				def result = updateBuild(collection, project, buildDef.id, deleteUnwantedTasks)
				if (result == null) {
					log.debug("BuildManagementService::updateBuild -- Failed to update build def ${buildDef.name}.")
				}
			}
		}
		return
	}

	public def updateBuild(def collection, def project, def buildId, boolean deleteUnwantedTasks) {
		log.debug("BuildManagementService::updateBuild -- Evaluating build def ${buildId} for project ${project.name}")
		def query = ['api-version':'5.1']
		def buildDef = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${buildId}",
			query: query,
		)
		// don't want call update if no changes were made
		def changed = false
		def xldPublishTaskFound = false
		def xldCreateDARTaskFound = false
		def xldCDTaskGroupFound = false
		def xldMFTaskGroupFound = false
		def publishTaskIdx = -1
		def phases = buildDef.process.phases
		phases.each { phase ->
			def steps = phase.steps
			def versionVar = ""
			for (int idx = 0; idx < steps.size(); idx++) {
			//phase.steps.each { step ->
				// set new values ,etc.
				def step = steps[idx]
				if (step.task.id == "218eff04-a485-4087-b005-e1f04527654d") {
					versionVar = step.inputs.OutputVariable
					if (versionVar != "build.buildnumber") {
						log.debug("Setting OutputVariable for tagging task ...")
						step.inputs.OutputVariable = "build.buildnumber"
						changed = true
					}
				}
				if (step.task.id == "ac4ee482-65da-4485-a532-7b085873e532" && step.inputs.goals == "versions:set") {
					if (versionVar != "build.buildnumber") {
						def options = step.inputs.options
						log.debug("Maven versions:set options = '${options}'")
						log.debug("Replacing '${versionVar}' with 'build.buildnumber' in options ...")
						def optionsVar = options.replace("${versionVar}", "build.buildnumber")
						//log.debug("Maven versions:set options now = '${optionsVar}'")
						step.inputs.options = optionsVar
						//step.inputs.options.replace("'${versionVar}'", 'build.buildnumber')
						changed = true
					}
				}
				// remove UDeploy: createVersion & addVersionFiles tasks
				if (step.task.id == "d7b8f29f-640e-4e08-926b-de4e265b6742") {
					if (deleteUnwantedTasks) {
						log.debug("Removing ${step.displayName} task ...")
						steps.remove(idx)
						idx--
						changed = true
					} else {
						if (versionVar != "build.buildnumber") {
							def args = step.inputs.udClientCommandArgs
							log.debug("Replacing '${versionVar}' with 'build.buildnumber' in udClientCommandArgs ...")
							def newArgs = args.replace("${versionVar}", "build.buildnumber")
							step.inputs.udClientCommandArgs = newArgs
							changed = true
						}
						if (step.enabled == true) {
							log.debug("Disabling ${step.displayName} task ...")
							step.enabled = false
							changed = true
						}
					}
				}
				// remove UDeploy: Copy Files task for udclient_runAppProc.json file
				if (step.task.id == "5bfb729a-a7c8-4a78-a7c3-8d717bb7c13c" && step.inputs.Contents == "udclient_runAppProc.json") {
					if (deleteUnwantedTasks) {
						log.debug("Removing Copy Files task for udclient_runAppProc.json file ...")
						steps.remove(idx)
						idx--
						changed = true
					} else {
						if (step.enabled == true) {
							log.debug("Disabling Copy Files task for udclient_runAppProc.json file ...")
							step.enabled = false
							changed = true
						}
					}
				}
				// check for XLDeploy: checkout-deployit-manifest task group
				//if (step.task.id == "d63f92e6-ffe7-4ab0-b034-70d75bb16903") {
				if (step.task.id == "1473e5ab-d932-4681-b8b9-023f7de6e49c") {
					xldMFTaskGroupFound = true
				}
				// Replace the currently used variable with 'build.buildnumber' for the Publish to XL Deploy task
				if (step.task.id == "c36fc88a-b479-461f-8067-8c3254af356c") {
					if (step.enabled == true && xldMFTaskGroupFound) {
						xldPublishTaskFound = true
						def vNum = step.inputs.versionNumber
						if (vNum != "\$(build.buildnumber)") {
							log.debug("Replacing versionNumber option with 'build.buildnumber' for Publish to XL Deploy task  ...")
							step.inputs.versionNumber = "\$(build.buildnumber)"
							changed = true
						}
					} else {
						// Remove the Publish to XL Deploy task if disabled
						log.debug("Removing the Publish to XL Deploy task because it is either disabled or the XLDeploy: checkout-deployit-manifest task group was not found.")
						steps.remove(idx)
						idx--
						changed = true
					}
				}
				// Remove the Create DAR package task and replace with the Create Dar Package task group
				if (step.task.id == "6d391a67-a4c0-4c48-9472-cfe5319b45f6") {
					if (xldMFTaskGroupFound) {
						xldCreateDARTaskFound = true
					} else {
						log.debug("Removing ${step.displayName} task ...")
						steps.remove(idx)
						idx--
						changed = true
					}
				}
				// check for XLDeploy: Create Dar Package task group
				if (step.task.id == "77dc638f-f104-4cc0-9ac9-ed0509f17c32") {
					xldCDTaskGroupFound = true
				}
				// Capture index of Publish Artifact: drop task
				if (step.task.id == "2ff763a7-ce83-4e1f-bc89-0ae63477cebe") {
					publishTaskIdx = idx
				}
			}
			// Add XLDeploy: Create Dar Package task group if not found
			if (!xldCDTaskGroupFound && !(xldMFTaskGroupFound && (xldPublishTaskFound || xldCreateDARTaskFound))) {
				log.debug("Adding XLDeploy: Create Dar Package task group for phase ${phase.name} ...")
				def jsonSlurper = new JsonSlurper()
				def xldtask = jsonSlurper.parseText '''
					{ "environment": {},
					  "enabled": true,
					  "continueOnError": false,
					  "alwaysRun": false,
					  "displayName": "Task group: XLDeploy: Create Dar Package ",
					  "timeoutInMinutes": 0,
					  "condition": "succeeded()",
					  "task": {
						 "id": "77dc638f-f104-4cc0-9ac9-ed0509f17c32",
						 "versionSpec": "1.*",
						 "definitionType": "metaTask"
					  },
					  "inputs": {}
					}'''
				if (publishTaskIdx == -1) {
					steps.add(xldtask)
				} else {
					steps.add(publishTaskIdx, xldtask)
				}
				
				
				
				changed = true
			}
			if (phase.target.allowScriptsAuthAccessOption == false) {
				log.debug("Setting allowScriptsAuthAccessOption to true ...")
				phase.target.allowScriptsAuthAccessOption = true
				changed = true
			}
		}
		// save changes
		if (changed) {
			return updateBuildDefinition(collection, project, buildDef)
		} else {
			return ""
		}
			
	}

	public def updateTaggingTasks(def collection, def project, def newOutputVar) {
		def buildDefs = getBuilds(collection, project)
		buildDefs.value.each { buildDef ->
			// HACK ALERT! Should try to pass this in as a parameter
			// For Digital Banking -- only process release builds and skip d3, d3z and zbc builds
			//if (buildDef.name.endsWith("-release") && (!buildDef.name.startsWith("d3") && !buildDef.name.startsWith("zbc"))) {
			if (buildDef.name.endsWith("-release")) {
				def result = updateTaggingTask(collection, project, buildDef.id, newOutputVar)
				if (result == null) {
					log.debug("BuildManagementService::updateBuild -- Failed to update build def ${buildDef.name}.")
				}
			}
		}
		return
	}

	public def updateTaggingTask(def collection, def project, def buildId, def outputVarName) {
		def query = ['api-version':'5.1']
		def buildDef = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${buildId}",
			query: query,
		)
		// don't want call update if no changes were made
		def changed = false
		def taggingTaskFound = false
		def xldMFTaskGroupFound = false
		def xldPublishTaskFound = false
		log.debug("BuildManagementService::updateTaggingTask -- Evaluating build def ${buildDef.name} for project ${project.name}")
		def phases = buildDef.process.phases
		phases.each { phase ->
			def steps = phase.steps
			def versionVar = ""
			for (int idx = 0; idx < steps.size(); idx++) {
			//phase.steps.each { step ->
				def step = steps[idx]
				// set new OutputVariable value for tagging task.
				if (step.task.id == "218eff04-a485-4087-b005-e1f04527654d") {
					taggingTaskFound = true
					versionVar = step.inputs.OutputVariable
					if (versionVar.toLowerCase() == "build.buildnumber") {
						log.debug("Setting OutputVariable for tagging task to ${outputVarName} ...")
						step.inputs.OutputVariable = "${outputVarName}"
						changed = true
					}
				}
				if (step.task.id == "ac4ee482-65da-4485-a532-7b085873e532" && step.inputs.goals == "versions:set") {
					if (versionVar.toLowerCase() == "build.buildnumber") {
						def options = step.inputs.options
						log.debug("Maven versions:set options = '${options}'")
						log.debug("Replacing '${versionVar}' with '${outputVarName}' in options ...")
						// Need this to catch case differences in name used, ie. Build.BuildNumber, build.buildnumber
						//def optionsVar = options.replace("Build.BuildNumber", "${outputVarName}")
						def optionsVar = options.replace("${versionVar}", "${outputVarName}")
						//log.debug("Maven versions:set options now = '${optionsVar}'")
						step.inputs.options = optionsVar
						changed = true
					}
				}
				// replace UDeploy: createVersion & addVersionFiles tasks
				/* Shouldn't need this as uDeploy deployments have been deprecated
				if (step.task.id == "d7b8f29f-640e-4e08-926b-de4e265b6742") {
					if (versionVar.toLowerCase() == "build.buildnumber") {
						def args = step.inputs.udClientCommandArgs
						log.debug("Replacing '${versionVar}' with '${outputVarName}' in udClientCommandArgs ...")
						def newArgs = args.replace("${versionVar}", "${outputVarName}")
						step.inputs.udClientCommandArgs = newArgs
						changed = true
					}
				}*/
				// check for XLDeploy: checkout-deployit-manifest task group
				// *** This task group will have a different ID for every project
				//if (step.task.id == "d63f92e6-ffe7-4ab0-b034-70d75bb16903") {
				// This task ID is for Digital Banking project
				//if (step.task.id == "1473e5ab-d932-4681-b8b9-023f7de6e49c") {
				//	xldMFTaskGroupFound = true
				//}
				// Replace the currently used variable with 'build.buildnumber' for the Publish to XL Deploy task
				if (step.task.id == "c36fc88a-b479-461f-8067-8c3254af356c") {
					xldPublishTaskFound = true
					//if (step.enabled == true && xldMFTaskGroupFound) {
					//if (step.enabled == true) {
					if (step.inputs.version == "true") {
						String vNum = step.inputs.versionNumber
						if (versionVar == "zions.buildnumber") {						
							//if (vNum.toLowerCase() != "\$(${versionVar})") {
							// check for blank instead
							if (vNum.replaceAll("\\s","") == "") {
								log.debug("Replacing versionNumber option with '${versionVar}' for Publish to XL Deploy task  ...")
								//log.debug("Replacing versionNumber option with '${outputVarName}' for Publish to XL Deploy task  ...")
								//step.inputs.versionNumber = "\$(${outputVarName})"
								step.inputs.versionNumber = "\$(${versionVar})"
								changed = true
							}
						}
					/*} else {
						// Remove the Publish to XL Deploy task if disabled
						log.debug("Removing the Publish to XL Deploy task because it is either disabled or the XLDeploy: checkout-deployit-manifest task group was not found.")
						steps.remove(idx)
						idx--
						changed = true
					*/
					}
				}
			}
		}
		// save changes
		if (changed) {
			return updateBuildDefinition(collection, project, buildDef)
		} else {
			return ""
		}
			
	}

	def updateBuildDefinition(def collection, def project, def bDef, def query = null) {
		def body = new JsonBuilder(bDef).toPrettyString()
		//println body
		log.debug("BuildManagementService::updateBuildDefinition --> ${body}")
		
		def result = null
		if (query) {
			result = genericRestClient.put(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${bDef.id}",
				body: body,
				query: query,
				headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true']
			)
		} else {
			result = genericRestClient.put(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions/${bDef.id}",
				body: body,
				headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true'],
			)
		}
		return result
	}
	
	public def getBuilds(def collection, def project) {
		log.debug("BuildManagementService::getBuilds for project = ${project.name}")
		//def query = ['name':"*${name}"]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/build/definitions",
			headers: [accept: 'application/json;api-version=5.1;excludeUrls=true'],
		)
		if (result == null || result.count == 0) {
			log.debug("BuildManagementService::getBuilds -- No build defs found for project ${project.name}.")
		}
		return result
	}

	public def getBuildById(def collection, def project, def id) {
		log.debug("BuildManagementService::getBuildById -- ID = " + id)
		def query = ['api-version':'5.1']
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
			log.debug("BuildManagementService::getResource -- Resource name provided: " + resourceName)
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

	public def getYamlBuildFilename( def collection, def repo, def branchName ) {
		def yamlFilename = null
		String[] yamlFilenames = yamlFilenamesStr.split(',')
		for(String fileName in yamlFilenames) {
			def aFile = codeManagementService.getFileContent(collection, repo.project, repo, fileName, branchName)
			if (aFile != null) {
				log.debug("BuildManagementService::getYamlBuildFilename -- YAML file found: " + fileName)
				yamlFilename = fileName
				break;
			}
		}
		return yamlFilename
	}

	private def getLatestTag(def collection, def project, def repo) {
		log.debug("BuildManagementService::getLatestTag for project ${project.name} and repo ${repo.name}")
		def query = ['filter':'tags', 'api-version':'4.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/refs",
			query: query
		)
		def resultCount = 0
		if (result != null) {
			resultCount = result.count
		}
		if (resultCount == 0) {
			log.debug("BuildManagementService::getLatestTag -- No tags found for project ${project.name} and repo ${repo.name}")
			return null
		}
		def lastTagIdx = resultCount - 1
		def latestTag = result.value[lastTagIdx]
		// check tag name to ensure that it represents a valid semantic version string
		def tagName = latestTag.name
		tagName = tagName.substring("refs/tags/".length(), tagName.length())
		while (notValidTag(tagName) && lastTagIdx > 0) {
			lastTagIdx--
			latestTag = result.value[lastTagIdx]
			tagName = latestTag.name
			tagName = tagName.substring("refs/tags/".length(), tagName.length())
		}

		return latestTag
	}

	private boolean notValidTag(def tagName) {
		log.debug("BuildManagementService::notValidTag -- Validing Tag ${tagName}")
		String tag = tagName
		if (tag.indexOf("-") > -1) {
			// look for tag with qualifier
			String[] tagParts = tag.split("-")
			tag = tagParts[0]
		}
		String[] versionParts = tag.split("\\.")

		if (versionParts.length < 3) {
			log.debug("BuildManagementService::notValidTag -- Did not find all 3 parts for version number")
			return true
		}
		for (int idx = 0; idx++; idx < 3) {
			try {
				Integer.parseInt(versionParts[idx])
			} catch (NumberFormatException e) {
				// Not a number so version string is invalid
				log.debug("BuildManagementService::notValidTag -- Version part " + idx + ", '${versionParts[idx]}' is not a number")
				return true
			}
		}
		return false
	}

	private def createInitialBuildTag(def collection, def project, def repo, def latestTag, def branchName) {
		String versionTag = latestTag.name.substring("refs/tags/".length())
		log.debug("BuildManagementService::createInitialBuildTag  -- versionTag = ${versionTag}")
		if (versionTag.indexOf("-") > -1) {
			// look for tag with qualifier
			String[] tagParts = versionTag.split("-")
			versionTag = tagParts[0]
		}
		def parts = versionTag.tokenize(".")
		def version = parts[0] + '.' + parts[1] + '.' + parts[2]
		log.debug("BuildManagementService::createInitialBuildTag  -- Version = ${version}")
		// check for node build to ensure that the build number is formatted correctly
		def buildType = BuildType.NONE
		buildType = detectBuildType(collection, project, repo)
		// use IFB branch name as qualifier
		def begIdx = -1
		if (branchName.toLowerCase().startsWith("refs/heads/ifb/")) {
			begIdx = "refs/heads/ifb/".length()
		} else {
			begIdx = "refs/heads/feature/ifb/".length()
		}
		def qualifier = branchName.substring(begIdx)
		log.debug("BuildManagementService::createInitialBuildTag  -- qualifier = ${qualifier}")
		def bldnum = "-00000"
		if (buildType == BuildType.NODE) {
			log.debug("BuildManagementService::createInitialBuildTag -- Detected node build type")
			bldnum = ".0"
		}
		def nTag = version + "-" + qualifier + bldnum;
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
