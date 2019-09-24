package com.zions.vsts.services.cli.action.build;

import java.lang.reflect.Field
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.work.WorkManagementService
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
	public ZeusBuildData() {
	}

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
		def buildWorkitems = buildManagementService.getExecutionWorkItems(collection, project, buildId)
		List wi = []
		buildWorkitems.'value'.each { ref ->
			wi.push("${ref.id}")
		}
		if (wi.empty) {
			log.error("Build has no new work items!  Usually do to no new changes since prior build.")
			System.exit(1)
		}
		def wis = wi.toSet()
		def buildChanges = buildManagementService.getExecutionChanges(collection, project, buildId, true)
		def fList = []
		def affiliates = []
		def allChanges = [:]
		def dList = []
		buildChanges.'value'.each { bchange ->
			if (bchange.location) {
				String url = "${bchange.location}/changes"
				def changes = buildManagementService.getExecutionResource(url)
				changes.changes.each { change ->
					String fpath = "${change.item.path}"
					String changeType = "${change.changeType}"
					ZeusBuildData.log.info "Type : $changeType, Name: ${fpath.substring(1)}"
					if (changeType == 'delete' && !dList.contains("${fpath.substring(1)}")) {
						dList.push("${fpath.substring(1)}")
					}
					if (!dList.contains("${fpath.substring(1)}") && change.item.path && !change.item.isFolder && !fpath.startsWith('/dar') && !fpath.contains('.gitignore') && !fpath.contains('.project') && !fpath.contains('.keep')) {
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
		def build = buildManagementService.getExecution(collection, project, buildId)
		String sourceBranch = "${build.sourceBranch}"
		//if (sourceBranch.contains("release/")) {
		String releaseId = sourceBranch.substring(sourceBranch.lastIndexOf('/')+1)
		def fListSet = fList.toSet()
		File f = new File("${outDir}/ZEUS.properties")
		def o = f.newDataOutputStream()
		o << "my.version=${releaseId}${sep}"
		o << "change.request=${changeRequest}${sep}"
		String affiliatesStr = affiliatesList.join(',')
		o << "global.affiliates.list=${affiliatesStr}${sep}"
		if (wis.size() > 0) {
			String wiStr = wis.join(',')
			o << "ado.workitems=${wiStr}${sep}"
		}
		o.close()
		f = new File("${outDir}/ZEUS.template")
		o = f.newDataOutputStream()
		String filesStr = fListSet.join("${sep}")
		o << "${filesStr}"
		o.close()
		Map<String, File> fileMap = [:]
		if (inRepoDir && outRepoDir) {
			File od = new File(outRepoDir)
			if (!od.exists()) {
				od.mkdir()
			}
			fListSet.each { String iName ->
				String fName = "/${iName}"
				def i = new File("$inRepoDir${fName}").newDataInputStream()
				def opath = fName.substring(0, fName.lastIndexOf('/'));
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
					File f = fileMap["${outRepoDir}${fName}"]
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
						if (wi.fields.'Microsoft.VSTS.Common.ResolvedReason') {
							r = "${wi.fields.'Microsoft.VSTS.Common.ResolvedReason'}"
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