package com.zions.pipeline.services.cli.action.build;

import java.lang.reflect.Field
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.pipeline.services.cli.action.build.ZeusBuildData
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.xld.services.ci.CIService
import com.zions.xld.services.deployment.DeploymentService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import groovy.time.TimeCategory


/**
 * @author z091182
 *
 *@startuml ZeusDesign.png
 *
 *cloud DTS as "ADO DTS Project" {
 *	storage serviceframework as "zions-service-framework GIT repo" {
 *		component ZeusBuildData as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-vsts-cli%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fvsts%2Fservices%2Fcli%2Faction%2Fbuild%2FZeusBuildData.groovy&version=GBdng2ado com.zions.vsts.services.cli.action.build.ZeusBuildData]] script"
 *	}
 *	storage retools as "re-tools GIT repo" {
 *		component ZeusAntScript as "[[https://dev.azure.com/zionseto/DTS/_git/re-tools?path=%2Fbin%2Fzeus%2Fzeusui3.xml&version=GBmaster zeusui3.xml]] ant script"
 *	}
 *  ZeusBuildData --> buildArtifacts: "Creates"
 *}
 *cloud ZeusProjectPipeline as "ADO Zeus Project - Pipeline" {
 *  storage buildArtifacts as "[[https://dev.azure.com/zionseto/Zeus/_apps/hub/ms.vss-ciworkflow.build-ci-hub?_a=edit-build-definition&id=973 Zeus-release]] - Build Artifacts" {
 *  	component zeusProperties as "Zeus.properties"
 *  	component zeusTemplate as "Zeus.template"
 *  	component zeusDetailXml as "Zeus.detail.xml"
 *  	component zeusRepoZip as "Zeus.repo.zip"
 *  }
 *}
 *cloud Zeus as "ADO Zeus Project"{
 *  rectangle Bug as "'Bug' work item type"
 *	actor Zeus_Developer as "Zeus Developer"
 *	component ZeusPipeline as "[[https://dev.azure.com/zionseto/Zeus/_apps/hub/ms.vss-ciworkflow.build-ci-hub?_a=edit-build-definition&id=1238 Zeus]] build pipeline"
 *	storage Zeus_GIT_repo as "Zeus GIT repo" {
 *		rectangle release_branch as "release/<release id> git branch"
 *  }
 *  Bug --> Zeus_Developer: Assigned to
 *  Zeus_Developer --> release_branch : "Provides pull request to release branch"
 *  Zeus_GIT_repo --> ZeusPipeline : "When repo changes build activates"
 *  ZeusPipeline --> ZeusBuildData : "Generates all build artifacts.\nPerforms some release tracking."
 *}
 *card XLDeploy as "XL Deploy" {
 *	component ZeusApp as "Applications/Zeus/Releases/<Release Id>/Zeus_<build number>_app" 
 *	component ZeusEnv as "Environments/Zeus/Releases/<Release Id>/QA/QA Email\nEnvironments/Zeus/Releases/<Release Id>/QA/QA Copy\nEnvironments/Zeus/Releases/<Release Id>/QAAuto/QAAuto Email\nEnvironments/Zeus/Releases/<Release Id>/QAAuto/QAAuto Copy\nEnvironments/Zeus/Releases/<Release Id>/UAT/UAT Email\nEnvironments/Zeus/Releases/<Release Id>/UAT/UAT Copy\nEnvironments/Zeus/Releases/<Release Id>/BL/BL EMail\nEnvironments/Zeus/Releases/<Release Id>/BL/BL Copy" 
 *	ZeusPipeline --> XLDeploy : "Creates/Updates Deployment CIs"
 *	ZeusPipeline --> ZeusApp : "Publish Deployment Package to App"
 *  ZeusApp --> ZeusAntScript : "Specific application package makes call to ant script for specified environment"
 *}
 *card XLR as "XL Release" {
 *  actor ReleaseManager as "Release Manager/QA/UAT"
 *  component ZeusTemplate as "Zeus Release Template"
 *  component ZeusRelease as "Zeus Release"
 *  ReleaseManager --> ZeusRelease: "Handles requests for response"
 *  ZeusRelease --> ZeusTemplate: "Template Used"
 *  ZeusPipeline --> XLR: "Creates release from updated template"
 *  ZeusRelease --> XLDeploy : "Request deploy to environment"
 *}
 *Zeus --[hidden]> XLDeploy
 *
 *@enduml
 */
@Component
@Slf4j
class ZeusBuildData implements CliAction {

	@Autowired
	BuildManagementService buildManagementService
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	CodeManagementService codeManagementService
	@Autowired
	CIService cIService
	@Autowired
	DeploymentService deploymentService

	public ZeusBuildData() {
	}

	@Value('${release.id:}')
	String releaseId

	@Value('${rollup:false}')
	boolean rollup

	@Value('${create.branch:true}')
	boolean createBranch

	@Value('${split.affiliates:true}')
	boolean splitAffiliates
	
	@Value('${all.affiliates:AZ-NBA,CA-CBT,CO-VBC,NV-NSB,TX-ABT,UT-ZFNB}')
	String[] allAffiliates

	@Value('${final.release.environment:Environments/Zeus/Releases/2305/BL/BL Promote}')
	String finalReleaseEnv

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String buildId = data.getOptionValues('build.id')[0]
		String outDir = data.getOptionValues('out.dir')[0]
		String inRepoDir = null
		try {
			inRepoDir = data.getOptionValues('in.repo.dir')[0]
		} catch (e) {}
		String outRepoDir = null
		try {
			outRepoDir = data.getOptionValues('out.repo.dir')[0]
		} catch (e) {}
		String changeRequest = null
		try {
			data.getOptionValues('change.request')[0]
		} catch (e) {}
		String releaseDate = null
		try {
			releaseDate = data.getOptionValues('release.date')[0]
		} catch (e) {}
		def build = buildManagementService.getExecution(collection, project, buildId)
		String sourceBranch = "${build.sourceBranch}"
		String repoId = "${build.repository.id}"

		def releases = getDevProdReleases(collection, project, repoId, createBranch)
		def gversions = []
		String prodRelease = null
		if (releases.prod) {
			String bName = "${releases.prod.name}"
			String v = bName.substring(8)
			gversions.add(v)
			prodRelease = v
			println "##vso[task.setvariable variable=prodRelease]${v}"
		}
		boolean provisionSetup = false
		if (releases.dev) {
			String bName = "${releases.dev.name}"
			String v = bName.substring(8)
			gversions.add(v)
			gversions.add("${gversions[0]}PR")
			println "##vso[task.setvariable variable=devRelease]${v}"
			String appId = "Applications/Zeus/Releases/${v}/Zeus_${v}_Provision"
			String environmentId = "Environments/Zeus/Releases/${v}/Testbed/Provision"
			def devTestBedProvisioning = cIService.getCI(appId)
			if (devTestBedProvisioning) {
				boolean hasDeploy = deploymentService.hasDeployment(appId, environmentId)
				if (hasDeploy) {
					println "##vso[task.setvariable variable=provisionSetup]true"
					provisionSetup = true
				} else {
					println "##vso[task.setvariable variable=provisionSetup]false"
				}
			} else {
				println "##vso[task.setvariable variable=provisionSetup]false"
			}
		}

		//if (sourceBranch.contains("release/")) {
		String releaseIdNormal = ''
		if ((!releaseId || releaseId.size() == 0) && sourceBranch.contains('release/')) {
			releaseId = "${sourceBranch.substring(sourceBranch.lastIndexOf('/')+1)}"
		}

		//Setup crq and release date from Release work item with title of release id.
		//		def crqAndRelease = getCRQAndReleaseDate(releaseId)
		//		String changeRequest = crqAndRelease.CRQ
		//		String releaseDate = crqAndRelease.releaseDate
		def builds = null
		boolean isProductionBranch = "${releaseId}" == "${prodRelease}"
		if (rollup) {
			builds = buildManagementService.getRelatedBuilds(collection, project, build, isProductionBranch)
		}
		def buildWorkitems = null
		if (!builds) {
			buildWorkitems = buildManagementService.getExecutionWorkItems(collection, project, buildId)
		} else {
			buildWorkitems = buildManagementService.getExecutionWorkItemsByBuilds(collection, project, builds)

		}
		List wi = []
		buildWorkitems.each { ref ->
			wi.push("${ref.id}")
		}
		if (wi.empty) {
			println "##vso[task.setvariable variable=hasChanges]false"
			log.error("Build has no new work items!  Usually do to no new changes since prior build.")
			return null
		}
		def wis = wi.toSet()
		def buildChanges = null
		if (!builds) {
			buildChanges = buildManagementService.getExecutionChanges(collection, project, buildId, true)
		} else {
			buildChanges = buildManagementService.getExecutionChangesByBuilds(collection, project, builds, true)
		}
		def fList = []
		def fListWFolders = []
		def affiliates = []
		def allChanges = [:]
		def dList = []
		//load keeps
		File iRepo = new File(inRepoDir)
		iRepo.eachFileRecurse { File f ->
			String fPath = f.absolutePath
			fPath = fPath.substring(inRepoDir.length())
			if (fPath.endsWith('.keep')) {
				fListWFolders.push("${fPath.substring(1).replace("\\", "/")}")
			}
		}
		buildChanges.each { bchange ->
			if (bchange.location) {
				String url = "${bchange.location}/changes"
				def changes = buildManagementService.getExecutionResource(url)
				changes.changes.each { change ->
					String fpath = "${change.item.path}"
					String changeType = "${change.changeType}"
					ZeusBuildData.log.info "Type : $changeType, Name: ${fpath.substring(1)}"
					if (!fileExists(inRepoDir, "${fpath.substring(1)}") && !dList.contains("${fpath.substring(1)}")) {
						dList.push("${fpath.substring(1)}")
					}
					//					if (fpath.contains('.keep')) {
					//						fListWFolders.push(fpath.replace("\\", "/"))
					//					}
					if ( (change.item.path) && !dList.contains("${fpath.substring(1)}") && !change.item.isFolder && !fpath.startsWith('/.vs') && !fpath.startsWith('/imgs') && !fpath.startsWith('/xl') && !fpath.startsWith('/dar') && !fpath.contains('.gitignore') && !fpath.contains('.project') && !fpath.endsWith('.keep') && !fpath.contains('.yml') && !fpath.contains('.md')) {
						fListWFolders.push(fpath.replace("\\", "/"))
						fList.push(fpath.substring(1))
						String[] fItems = fpath.split('/')
						if (fItems.size() > 3) {
							if (!allChanges.containsKey(fpath)) {
								allChanges[fpath] = [ parent: bchange, item: change.item ]
							}
							String affiliate = fItems[2]

							affiliates.push(affiliate)
						} else {
							ZeusBuildData.log.info("Bad path:: ${fpath}" )
						}
					}
				}
			}
		}
		Set affiliatesList = affiliates.toSet()
		//		String sep = System.getProperty("line.separator")
		String sep = "\r\n"
		def fListSet = fList.toSet()
		File f = new File("${outDir}/ZEUS.properties")
		def o = f.newDataOutputStream()
		if (isProductionBranch) {
			o << "my.version=${releaseId}PR${sep}"

		} else {
			o << "my.version=${releaseId}${sep}"
		}
		o << "build.number=${build.buildNumber}${sep}"
		if (changeRequest) {
			o << "change.request=${changeRequest}${sep}"

		} else {
			o << "change.request={{change.request}}${sep}"
		}
		String affiliatesStr = affiliatesList.join(',')
		o << "global.affiliates.list=${affiliatesStr}${sep}"
		println "##vso[task.setvariable variable=activeAffiliates]${affiliatesStr}"
		
		if (wis.size() > 0) {
			String wiStr = wis.join(',')
			o << "ado.workitems=${wiStr}${sep}"
		}
		if (releaseDate) {
			o << "release.date=${releaseDate}${sep}"
		} else {
			o << "release.date={{release.date}}${sep}"

		}
		o << "global.versions.list=${gversions.join(',')}${sep}"
		if (gversions.size() == 1) {
			o << "uat.zeusdev.version=${gversions[0]}${sep}"
		}
		if (gversions.size() >= 2) {
			o << "uat.zeusprod.version=${gversions[0]}PR${sep}"
			o << "uat.zeusdev.version=${gversions[1]}${sep}"
			if (isProductionBranch) {
				o << "bl.zeusprod.version=${gversions[0]}PR${sep}"
			} else {
				o << "bl.zeusprod.version=${gversions[1]}${sep}"
			}
		}
		o.close()
		if (splitAffiliates) {
			allAffiliates.each { aff ->
				File od = new File("${outDir}/${aff}")
				if (!od.exists()) {
					od.mkdirs()
				}
				f = new File("${outDir}/${aff}/ZEUS.properties")
				o = f.newDataOutputStream()
				if (isProductionBranch) {
					o << "my.version=${releaseId}PR${sep}"

				} else {
					o << "my.version=${releaseId}${sep}"
				}
				o << "build.number=${build.buildNumber}${sep}"
				if (changeRequest) {
					o << "change.request=${changeRequest}${sep}"

				} else {
					o << "change.request={{change.request}}${sep}"
				}
				//String affiliatesStr = affiliatesList.join(',')
				o << "global.affiliates.list=${aff}${sep}"
				if (wis.size() > 0) {
					String wiStr = wis.join(',')
					o << "ado.workitems=${wiStr}${sep}"
				}
				if (releaseDate) {
					o << "release.date=${releaseDate}${sep}"
				} else {
					o << "release.date={{release.date}}${sep}"

				}
				o << "global.versions.list=${gversions.join(',')}${sep}"
				if (gversions.size() == 1) {
					o << "uat.zeusdev.version=${gversions[0]}${sep}"
				}
				if (gversions.size() >= 2) {
					o << "uat.zeusprod.version=${gversions[0]}PR${sep}"
					o << "uat.zeusdev.version=${gversions[1]}${sep}"
					if (isProductionBranch) {
						o << "bl.zeusprod.version=${gversions[0]}PR${sep}"
					} else {
						o << "bl.zeusprod.version=${gversions[1]}${sep}"
					}
				}
				o.close()
			}
		}
		if (fListSet.isEmpty()) {
			log.error("Build has no new files!  Usually do to no new changes since prior build.")
			println "##vso[task.setvariable variable=hasChanges]false"
			return null
		}
		println "##vso[task.setvariable variable=hasChanges]true"

		f = new File("${outDir}/ZEUS.template")
		def oFList = []
		fListSet.each { String fName ->
			String n = fName.substring(fName.indexOf('/')+1)
			oFList.push(n)
		}
		def ofListSet = oFList.toSet()
		o = f.newDataOutputStream()
		String filesStr = ofListSet.join("${sep}")
		o << "${filesStr}${sep}"
		o.close()
		if (splitAffiliates) {
			allAffiliates.each { aff ->
				f = new File("${outDir}/${aff}/ZEUS.template")
				oFList = []
				fListSet.each { String fName ->
					String n = fName.substring(fName.indexOf('/')+1)
					if (n.startsWith("${aff}/")) {
						oFList.push(n)
					}
				}
				ofListSet = oFList.toSet()
				o = f.newDataOutputStream()
				filesStr = ofListSet.join("${sep}")
				o << "${filesStr}${sep}"
				o.close()

			}
		}
		Map<String, File> fileMap = [:]
		if (inRepoDir && outRepoDir) {
			File od = new File(outRepoDir)
			if (!od.exists()) {
				od.mkdir()
			}
			def fListWFoldersSet = fListWFolders.toSet()
			fListWFoldersSet.each { String iName ->
				String fName = "/${iName}"
				def i = new File("$inRepoDir${fName}").newDataInputStream()
				def opath = fName.substring(0, fName.lastIndexOf("/"));
				File ofd = new File("$outRepoDir${opath}")
				if (!ofd.exists()) {
					ofd.mkdirs()
				}
				if (!fName.endsWith('.keep')) {
					File of = new File("$outRepoDir${fName}")
					def ao = of.newDataOutputStream()
					ao << i
					i.close()
					ao.close()
					fileMap["$outRepoDir${fName}"] = of
				}
			}
			detailsFile(collection, project, wis, allChanges, outDir, outRepoDir, fileMap, allAffiliates)
		}
		//}
		File xlrTemplateYaml = new File("${inRepoDir}/xl/xebialabs/xlr-template.yaml")
		//writeXLRTemplateYaml(affiliatesList, xlrTemplateYaml)
		return null
	}

	def getDevProdReleases(String collection, String project, String repoName, boolean createBranch) {
		def branches = codeManagementService.getBranches(collection, project, repoName)
		def releaseBranches = branches.'value'.findAll { branch ->
			String name = "${branch.name}"
			name ==~ /release\/\d{4}/
		}
		releaseBranches = releaseBranches.sort { a,b -> a.name <=>  b.name }
		def rBranches = [dev: null, prod: null]
		if (releaseBranches.size() == 1) {
			rBranches.dev = releaseBranches[0]
			rBranches.prod = null
		}
		if (releaseBranches.size() >= 2) {
			int size = releaseBranches.size()
			rBranches.dev = releaseBranches[size-1]
			rBranches.prod = releaseBranches[size-2]
		}
		rBranches = updateForBLRelease(rBranches, createBranch)
		return rBranches
	}

	def updateForBLRelease(def rBranches, boolean createBranch) {
		String bName = "${rBranches.dev.name}"
		String v = bName.substring(8)
		String appId = "Applications/Zeus/Releases/${v}/Zeus_${v}_App"
		String environmentId = finalReleaseEnv
		boolean hasDeploy = deploymentService.hasDeployment(appId, environmentId)
		if (hasDeploy) {
			rBranches.prod = rBranches.dev
			rBranches.dev = null
			if (createBranch) {
				Date cd = Date.parse('yyMM', v)
				Date nd = null
				TimeCategory t
				use(TimeCategory) {
					nd = cd + 3.months
					println nd

				}
				String name = "release/${nd.format('yyMM')}"
				def pBranch = codeManagementService.ensureBranch('', 'Zeus', 'Zeus', 'master', name)
				rBranches.dev = pBranch
			}
		}
		return rBranches
	}

	boolean fileExists(String inRepoDir, String iName) {
		String fName = "/${iName}"
		def i = new File("$inRepoDir${fName}")
		return i.exists()
	}
	
	def writeXLRTemplateYaml(affiliatesList, File inFile) {
		def reader = new StringReader(inFile.text)
		Representer r = new Representer()
		Yaml appYaml = new Yaml()  //TODO:  Study up on examples of MarkupBuilder
		
		appYaml.load(reader)
		def yamlMap = [apiVersion: 'xl-deploy/v1', kind: 'Applications', spec: []]
		def appZeus = [ directory: 'Application/Zeus', children: []]
		yamlMap.spec.add(appZeus)
	}

	def detailsFile(String collection, String project, def wis, def allChanges, outDir, outRepoDir, Map<String,File> fileMap, affiliatesList) {
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)  //TODO:  Study up on examples of MarkupBuilder
		File outFile = new File("${outDir}/ZEUS.details.xml")
		def fWorkitems = []
		wis.each { id ->
			String sId = "${id}"
			def fWorkItem = workManagementService.getWorkItem(collection, project, sId)
			fWorkitems.push(fWorkItem)
		}
		bXml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
		bXml.details {
			allChanges.each { key, change ->
				String fpath = "${change.item.path}"
				String[] fItems = fpath.split('/')
				String affiliate = fItems[2]
				entry(affiliate: "${affiliate}") {
					String fName = "${change.item.path}"
					File f = fileMap["${outRepoDir}/${fName}"]
					if (fName.startsWith('/')) {
						fName = fName.substring(1)
					}
					file ( "${fName}" )
					size ( "${f.size()}" )
					date ( "${change.parent.timestamp}" )
					gitCommit ( "${change.parent.id}" )
					String message = "${change.parent.message}"
					gitMessage {  bXml.mkp.yieldUnescaped("<![CDATA[${message}]]>") }
					fWorkitems.each { wi ->
						adoId ( "${wi.id}" )
						adoTitle ( "${wi.fields.'System.Title'}" )
						adoState ( "${wi.fields.'System.State'}" )
						String r = ""
						if (wi.fields.'System.Reason') {
							r = "${wi.fields.'System.Reason'}"
						}
						adoResolution ( "${r}" )
						adoType ( "${wi.fields.'System.WorkItemType'}" )
					}

				}
			}
		}
		def io = outFile.newDataOutputStream()
		io << writer.toString()
		io.close();
		if (splitAffiliates) {
			affiliatesList.each { aff ->
				writer = new StringWriter()
				bXml = new MarkupBuilder(writer)  //TODO:  Study up on examples of MarkupBuilder
				outFile = new File("${outDir}/${aff}/ZEUS.details.xml")
				bXml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
				boolean hasChanges = false
				bXml.details {
					allChanges.each { key, change ->
						String fpath = "${change.item.path}"
						String[] fItems = fpath.split('/')
						String affiliate = fItems[2]
						if (affiliate == aff) {
							String fName = "${change.item.path}"
							File f = fileMap["${outRepoDir}/${fName}"]
							if (fName.startsWith('/')) {
								fName = fName.substring(1)
							}
							file ( "${fName}" )
							size ( "${f.size()}" )
							date ( "${change.parent.timestamp}" )
							gitCommit ( "${change.parent.id}" )
							String message = "${change.parent.message}"
							gitMessage {  bXml.mkp.yieldUnescaped("<![CDATA[${message}]]>") }
							fWorkitems.each { wi ->
								adoId ( "${wi.id}" )
								adoTitle ( "${wi.fields.'System.Title'}" )
								adoState ( "${wi.fields.'System.State'}" )
								String r = ""
								if (wi.fields.'System.Reason') {
									r = "${wi.fields.'System.Reason'}"
								}
								adoResolution ( "${r}" )
								adoType ( "${wi.fields.'System.WorkItemType'}" )
							}

						}
					}
				}
				io = outFile.newDataOutputStream()
				io << writer.toString()
				io.close();
			}
		}


	}

	private def getCRQAndReleaseDate(String releaseId) {
		def out = [CRQ: 'NotSet', releaseDate: 'UNKNOWN']
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = 'Zeus' AND [System.AreaPath] under 'Zeus' AND [System.WorkItemType] = 'Release' and [System.Title] = '${releaseId}'"
		def wis = workManagementService.getWorkItems('', 'Zeus', query)
		if (wis.workItems && wis.workItems.size() >= 1) {
			String crq = 'NotSet'
			def wi = wis.workItems[0]
			wi = workManagementService.getWorkItem(wi.url)
			String crqs = "${wi.fields.'Custom.CRQs'}"
			if (crqs && crqs != 'null') {
				def crqList = crqs.split(',')
				if (crqList.size() > 0) {
					crq = crqList[crqList.size()-1]
				}
			}
			String releaseDate = 'UNKNOWN'
			String rDate = "${wi.fields.'System.ChangedDate'}"
			if (rDate && rDate != 'null') {
				rDate = rDate.substring(0, "yyyy-MM-dd".length())
				Date modDate = Date.parse("yyyy-MM-dd", rDate)
				rDate = modDate.format('yyyyMMdd')
				releaseDate = rDate
			}
			if (crq != 'NotSet') {
				out.CRQ = crq
				out.releaseDate = releaseDate
			}
		}
		return out
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'build.id', 'out.dir', 'change.request']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}