package com.zions.vsts.services.cli.action.build;

import java.lang.reflect.Field
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.xld.services.ci.CIService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder


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
 *	component ZeusPipeline as "[[https://dev.azure.com/zionseto/Zeus/_apps/hub/ms.vss-ciworkflow.build-ci-hub?_a=edit-build-definition&id=973 Zeus-release]] build pipeline"
 *	storage Zeus_GIT_repo as "Zeus GIT repo" {
 *		rectangle release_branch as "release/<release id> git branch"
 *  }
 *  Bug --> Zeus_Developer: Assigned to
 *  Zeus_Developer --> release_branch : "Provides pull request to release branch"
 *  ZeusPipeline --> Zeus_GIT_repo : "When repo changes build activates"
 *  ZeusPipeline --> ZeusBuildData : "Generates all build artifacts"
 *}
 *card XLDeploy as "XL Deploy" {
 *  actor ReleaseManager as "Release Manager"
 *	component ZeusApp as "Application/Zeus/Zeus" 
 *	ZeusPipeline --> ZeusApp : "Publish Deployment Package to App"
 *  ReleaseManager --> ZeusApp : "Request deploy to environment"
 *  ZeusApp --> ZeusAntScript : "Specific application package makes call to ant script for specified environment"
 *}
 *Zeus --[hidden]> XLDeploy
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
	public ZeusBuildData() {
	}
	
	@Value('${release.id:}')
	String releaseId
	
	@Value('${rollup:false}')
	boolean rollup

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
		String changeRequest = data.getOptionValues('change.request')[0]
		String releaseDate = null
		try {
			releaseDate = data.getOptionValues('release.date')[0]
		} catch (e) {}
		def build = buildManagementService.getExecution(collection, project, buildId)
		String sourceBranch = "${build.sourceBranch}"
		String repoId = "${build.repository.id}"
		
		def releases = getDevProdReleases(collection, project, repoId)
		def gversions = []
		if (releases.prod) {
			String bName = "${releases.prod.name}"
			String v = bName.substring(8)
			gversions.add(v)
			println "##vso[task.setvariable variable=prodRelease]${v}"

		}
		boolean provisionSetup = false
		if (releases.dev) {
			String bName = "${releases.dev.name}"
			String v = bName.substring(8)
			gversions.add(v)
			println "##vso[task.setvariable variable=devRelease]${v}"
			def devTestBedProvisioning = cIService.getCI("Applications/Zeus_xld/Releases/${v}/Provision_TestBeds")
			if (devTestBedProvisioning) {
				println "##vso[task.setvariable variable=provisionSetup]true"
				provisionSetup = true 
			} else {
				println "##vso[task.setvariable variable=provisionSetup]false"
			}
		}
		
		//if (sourceBranch.contains("release/")) {
		String releaseIdNormal = ''
		if ((!releaseId || releaseId.size() == 0) && sourceBranch.contains('release/')) {
			releaseId = "${sourceBranch.substring(sourceBranch.lastIndexOf('/')+1)}"
		}
		def builds = null
		if (rollup) {
			builds = buildManagementService.getRelatedBuilds(collection, project, build)
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
		if (wi.empty && provisionSetup) {
			log.error("Build has no new work items!  Usually do to no new changes since prior build.")
			System.exit(1)
		}
		if (wi.empty && !provisionSetup) {
			return
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
					if ( (change.item.path) && !dList.contains("${fpath.substring(1)}") && !change.item.isFolder && !fpath.startsWith('/xl') && !fpath.startsWith('/dar') && !fpath.contains('.gitignore') && !fpath.contains('.project') && !fpath.contains('.keep') && !fpath.contains('.yml')) {
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
		String sep = System.getProperty("line.separator")
		def fListSet = fList.toSet()
		File f = new File("${outDir}/ZEUS.properties")
		def o = f.newDataOutputStream()
		o << "my.version=${releaseId}${sep}"
		o << "change.request={{change.request}}${sep}"
		String affiliatesStr = affiliatesList.join(',')
		o << "global.affiliates.list=${affiliatesStr}${sep}"
		if (wis.size() > 0) {
			String wiStr = wis.join(',')
			o << "ado.workitems=${wiStr}${sep}"
		}
		if (releaseDate) {
			o << "release.date=${releaseDate}${sep}"
		}
		o << "global.versions.list=${gversions.join(',')}${sep}"
		if (gversions.size() == 1) {
			o << "uat.zeusdev.version=${gversions[0]}${sep}"
		} 
		if (gversions.size() == 2) {
			o << "uat.zeusprod.version=${gversions[0]}${sep}"
			o << "uat.zeusdev.version=${gversions[1]}${sep}"
			o << "bl.zeusprod.version=${gversions[0]}${sep}"
		}
		o.close()
		if (fListSet.isEmpty() && provisionSetup) {
			log.error('No files set for update! No new changes.')
			System.exit(1)
		}

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
				File of = new File("$outRepoDir${fName}")
				def ao = of.newDataOutputStream()
				ao << i
				i.close()
				ao.close()
				fileMap["$outRepoDir${fName}"] = of
			}
			detailsFile(collection, project, wis, allChanges, outDir, outRepoDir, fileMap)
		}
		//}
		return null
	}
	
	def getDevProdReleases(String collection, String project, String repoName) {
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
		return rBranches
	}

	boolean fileExists(String inRepoDir, String iName) {
		String fName = "/${iName}"
		def i = new File("$inRepoDir${fName}")
		return i.exists()
	}

	def detailsFile(String collection, String project, def wis, def allChanges, outDir, outRepoDir, Map<String,File> fileMap) {
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

		def o = outFile.newDataOutputStream()
		o << writer.toString()
		o.close();

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