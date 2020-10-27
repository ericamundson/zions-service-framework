package com.zions.pipeline.services.cli.action.build;

import java.lang.reflect.Field
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.pipeline.services.cli.action.build.PackageBuilder
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.xld.services.ci.CiService
import com.zions.xld.services.deployment.DeploymentService
import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.items.ReleaseItemService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.ho.yaml.Yaml
import groovy.time.TimeCategory
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * @author z091182
 *
 *@startuml ZeusDesign.svg
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
class PackageBuilder implements CliAction {

	@Autowired
	BuildManagementService buildManagementService
	@Autowired
	WorkManagementService workManagementService
	@Autowired
	CodeManagementService codeManagementService
	@Autowired
	CiService ciService
	@Autowired
	ReleaseQueryService releaseQueryService
	@Autowired
	DeploymentService deploymentService

	@Autowired
	ProjectManagementService projectManagementService

	public PackageBuilder() {
	}


	@Value('${rollup:true}')
	boolean rollup

	@Value('${create.branch:true}')
	boolean createBranch

	@Value('${final.release.environment:none}')
	String finalReleaseEnv

	@Value('${build.tag.filter:none}')
	String buildTagFilter

	@Value('${xlr.folder:none}')
	String xlrFolder

	@Value('${create.tag:false}')
	boolean createTag

	@Value('${wi.list:}')
	String[] wiList

	@Value('${ignore.list:}')
	String[] ignoreList

	@Value('${team.project:none}')
	String teamProject

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
		def build = buildManagementService.getExecution(collection, project, buildId)
		String sourceBranch = "${build.sourceBranch}"
		String repoId = "${build.repository.id}"
		def repo = build.repository
		def projectInfo = projectManagementService.getProject(collection, project)
		def fList = []
		def fListWFolders = []
		def dList = []
		//load keeps
		File iRepo = new File(inRepoDir)
		//		iRepo.eachFileRecurse { File f ->
		//			String fPath = f.absolutePath
		//			fPath = fPath.substring(inRepoDir.length())
		//			if (fPath.endsWith('.keep')) {
		//				fListWFolders.push("${fPath.substring(1).replace("\\", "/")}")
		//			}
		//		}
		def changes = codeManagementService.getChangesForWorkitems(collection, projectInfo, repo, wiList, sourceBranch)
		changes.each { change ->
			String fpath = "${change.item.path}"
			String changeType = "${change.changeType}"
			PackageBuilder.log.info "Type : $changeType, Name: ${fpath.substring(1)}"
			if (!fileExists(inRepoDir, "${fpath.substring(1)}") && !dList.contains("${fpath.substring(1)}")) {
				dList.push("${fpath.substring(1)}")
			}
			//					if (fpath.contains('.keep')) {
			//						fListWFolders.push(fpath.replace("\\", "/"))
			//					}
			//if ( (change.item.path) && !dList.contains("${fpath.substring(1)}") && !change.item.isFolder && !fpath.startsWith('/libs') && !fpath.startsWith('/batch') && !fpath.startsWith('/.vs') && !fpath.startsWith('/imgs') && !fpath.startsWith('/xl') && !fpath.startsWith('/dar') && !fpath.contains('.gitignore') && !fpath.contains('.project') && !fpath.endsWith('.keep') && !fpath.contains('.yml') && !fpath.contains('.md')) {
			if ( (change.item.path) && !dList.contains("${fpath.substring(1)}") && !change.item.isFolder && !ignore(fpath)) {
				fListWFolders.push(fpath.replace("\\", "/"))
				fList.push(fpath.substring(1))
			}
		}

		def fListSet = fList.toSet()
		if (fListSet.isEmpty()) {
			log.error("Build has no new files!  Usually do to no new changes since prior build.")
			println "##vso[task.setvariable variable=hasChanges]false"
			return null
		}
		println "##vso[task.setvariable variable=hasChanges]true"

		def oFList = []
		fListSet.each { String fName ->
			String n = fName.substring(fName.indexOf('/')+1)
			oFList.push(n)
		}
		def ofListSet = oFList.toSet()
		Map<String, File> fileMap = [:]
		if (inRepoDir && outRepoDir) {
			File od = new File(outRepoDir)
			if (!od.exists()) {
				od.mkdir()
			}
			def fListWFoldersSet = fListWFolders.toSet()
			for (String fName in fListWFoldersSet) {
			//fListWFoldersSet.each { String iName ->
				//String fName = "/${iName}"
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
		}
		//}
		//writeXLRTemplateYaml(affiliatesList, xlrTemplateYaml)
		return null
	}

	boolean includeBuild(data) {
		if (wiList == null || wiList.size() == 0 || (wiList.size() == 1 && wiList[0] == 'none')) return true
		List<String> theWIList = new ArrayList()
		theWIList.addAll(wiList)

		def bWis = data.workitems
		for (def wi in bWis) {
			String bwiId = "${wi.id}"
			if (theWIList.contains(bwiId)) return true
		}
		return false
	}


	boolean fileExists(String inRepoDir, String iName) {
		String fName = "/${iName}"
		def i = new File("$inRepoDir${fName}")
		return i.exists()
	}


	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.project', 'build.id', 'out.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

	boolean ignore(String fName) {
		if (ignoreList) {
			ignoreList.each { regexStr ->
				Matcher m = fName =~ regexStr
				if (m.find()) {
					return true
				}
			}
		}
		return false
	}

}