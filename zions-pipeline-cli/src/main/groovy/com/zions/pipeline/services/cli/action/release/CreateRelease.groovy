package com.zions.pipeline.services.cli.action.release
import org.springframework.boot.ApplicationArguments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.common.services.cli.action.CliAction
import com.zions.xld.services.ci.CiService
import static groovy.io.FileType.*
import groovy.json.JsonBuilder

import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.items.ReleaseItemService
import com.zions.xlr.services.events.db.XlrReleaseSubscription
import com.zions.xlr.services.events.db.XlrReleaseSubscriptionRepository

@Component
@Slf4j
class CreateRelease implements CliAction {
		
	@Value('${xlr.template.title:}')
	String templateTitle
	
	@Value('${xlr.release.variable.settings:}')
	String[] releaseVariableSettings
	
	@Value('${ado.project:}')
	String adoProject
	
	@Value('${ado.pipeline.id:}')
	String adoPipelineId

	@Value('${is.release.pipeline:false}')
	boolean isReleasePipeline

	@Value('${xlr.release.title:none}')
	String releaseTitle
	
	@Value('${xlr.fail.pipeline:false}')
	boolean xlrFailPipeline
	
	@Autowired
	ReleaseQueryService releaseQueryService
	
	@Autowired
	ReleaseItemService releaseItemService
	
	@Autowired(required=false)
	XlrReleaseSubscriptionRepository xlrReleaseSubscriptionRepository

	public def execute(ApplicationArguments data) {
		def templates = releaseQueryService.getTemplates(templateTitle, 0)
		if (templates && templates.size() == 1) {
			String json = new JsonBuilder(templates).toPrettyString()
			String templateId = releaseId(templates[0])
			String folderId = folderId(templates[0])
			log.info("ReleaseId:  ${templateId},  FolderId:  ${folderId}")
			def xlrData = [releaseTitle: "${releaseTitle}", folderId: "${folderId}"]
		
			if (releaseVariableSettings.size() > 0) {
				xlrData['variables'] = [:]
				for (String setting in releaseVariableSettings) {
					String[] nameval = setting.split('=')
					if (nameval.length == 2) {
						String name = '${' + nameval[0] + '}'
						xlrData.variables["${name}"] = "${nameval[1]}"
					}
				}
			}
			def xlrResult = releaseItemService.createRelease(templateId, xlrData)
			String releaseId = releaseId(xlrResult)
			String fReleaseId = "${xlrResult.id}"
			XlrReleaseSubscription subscription = new XlrReleaseSubscription([releaseId: fReleaseId, pipelineId: adoPipelineId, adoProject: adoProject, isReleasePipeline: isReleasePipeline, failPipeline: xlrFailPipeline])
			xlrReleaseSubscriptionRepository.save(subscription)
			xlrResult = releaseItemService.startPlannedRelease(releaseId)
		}
	}
	
	public Object validate(ApplicationArguments args) throws Exception {
	}
	
	String releaseId(def template) {
		String id = "${template.id}"
		return id.substring(id.lastIndexOf('/')+1)
	}
	
	String folderId(def template) {
		String id = "${template.id}"
		return id.substring(0, id.lastIndexOf('/'))
	}
}

