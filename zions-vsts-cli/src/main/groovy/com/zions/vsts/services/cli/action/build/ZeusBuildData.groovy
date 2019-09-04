package com.zions.vsts.services.cli.action.build;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class ZeusBuildData implements CliAction {
	
	@Autowired
	BuildManagementService buildManagementService
	
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
		String changeRequest = data.getOptionValues('change.request')[0]
		def buildWorkitems = buildManagementService.getExecutionWorkItems(collection, project, buildId)
		List wi = []
		buildWorkitems.'value'.each { ref ->
			wi.push("${ref.id}")
		}
		def wis = wi.toSet()
		def buildChanges = buildManagementService.getExecutionChanges(collection, project, buildId, true)
		def fList = []
		buildChanges.'value'.each { bchange -> 
			if (bchange.location) {
				String url = "${bchange.location}/changes"
				def changes = buildManagementService.getExecutionResource(url)
				changes.changes.each { change -> 
					if (change.item.path && !change.item.isFolder) {
						String fpath = "${change.item.path}"
						fList.push(fpath)
					}
					
				}
			}
		}
		
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
			String wiStr = wis.join(',')
			o << "ado.workitems=${wiStr}${sep}"
			o.close()
			f = new File("${outDir}/ZEUS.template")
			o = f.newDataOutputStream()
			String filesStr = fListSet.join("${sep}")
			o << "${filesStr}"
			o.close()
		//}
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project', 'build.id', 'out.dir', 'change.request']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}