package com.zions.vsts.services.attachments

import com.zions.common.services.attachments.IAttachments
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.FileManagementService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import com.zions.common.services.rest.IGenericRestClient

/**
 * Sends files to be used as attachments to ADO.
 * 
 * @author z091182
 *
 */
@Slf4j
@Component
class AttachmentManagementService implements IAttachments {
	
	@Autowired
	@Value('${tfs.project:none}')
	String tfsProject
	
	@Autowired
	@Value('${tfs.collection:}')
	private String tfsCollection;
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	@Autowired
	FileManagementService fileManagementService
	
	@Autowired
	TestManagementService testManagementService

	/* (non-Javadoc)
	 * @see com.zions.common.services.attachments.IAttachments#sendAttachment(java.lang.Object)
	 */
	public def sendAttachment(def info) {
		byte[] file = info.file
		if (file) {
		return fileManagementService.uploadAttachment(tfsCollection, this.tfsProject, this.tfsProject, file, file.fileName)
		} else {
			log.debug("sendAttachment was provided with a null file, skipping upload attempt")
			return null
		}
	}
	
	public def sendAttachment(byte[] content, String filename) {
		if (content) {
			return fileManagementService.uploadAttachment(tfsCollection, this.tfsProject, this.tfsProject, content, filename)
		} else {
			log.debug("sendAttachment was provided with a null file, skipping upload attempt")
			return null
		}
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.attachments.IAttachments#sendAttachment(java.lang.Object)
	 */
	public def ensureResultAttachments(def adoresult, def binaries, String rwebId) {
		if (binaries) {
			return testManagementService.ensureAttachments(adoresult, binaries, rwebId)
		} else {
			log.debug("No binaries sent")
			return null
		}
	}
	
	public def sendManualResultAttachment(adoResult, binary) {
		return testManagementService.sendManualResultAttachment(adoResult, binary)
	}

	public def getResAttDetails(collection, project, runId, resultId) {
		
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/test/Runs/${runId}/Results/${resultId}/?detailsToInclude=Iterations&api-version=6.0",
			headers: ['Content-Type': 'application/json'],
			
			)

		return result.iterationDetails.attachments

	}
	
	
	public def uploadResAttachment(collection, project, runId, resultId, stream, fileName, comment) {
		
	def eproject = URLEncoder.encode(project, 'utf-8')
	eproject = eproject.replace('+', '%20')
	
	def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Runs/${runId}/Results/${resultId}/attachments?api-version=6.0&bypassRules=True&suppressNotifications=true"
	def body = ['stream': stream, 'fileName': fileName, 'comment': comment, 'attachmentType': 'GeneralAttachment']
	String sbody = new JsonBuilder(body).toPrettyString()

	def result = genericRestClient.rateLimitPost(
				  
		requestContentType: ContentType.JSON,
		contentType: ContentType.JSON,
		uri: uri,
		body: sbody,
		
		query: ['api-version': '5.1-preview.1' ]
		)
	return result
	}
	
	public def downloadResAttachments(collection, project, runId, resultId, attachId) {
		
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		
		def result = genericRestClient.get(
			
			contentType: 'application/octet-stream',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/test/Runs/${runId}/Results/${resultId}/attachments/${attachId}?",
			headers: ['Content-Type': 'application/octet-stream'],
			query: ['api-version':'6.0-preview.1', 'expand': 'all']
			)

		return result;

	}
}
