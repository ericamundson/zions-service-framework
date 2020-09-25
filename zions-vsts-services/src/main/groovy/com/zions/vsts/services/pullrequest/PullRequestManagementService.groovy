package com.zions.vsts.services.pullrequest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import groovyx.net.http.ContentType

import groovy.util.logging.Slf4j

@Component
@Slf4j
class PullRequestManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	
	def createdCommentThread(String collection, def project, def repo, String pullRequestId, def commentData)
	{
		def body = [:]
		body.comments = []
		String commentType = 'text'
		String fullmessage = ""
		if (commentData.location) {
			commentType = 'codeChange'
		}
		for (String message in commentData.messages) {
			fullmessage += "${message}\n"
		}
		def mData = [parentCommentId: 0, content: fullmessage, commentType: commentType]
		body.comments.add(mData)
		body.status = 'active'
		if (commentData.location) {
			body.threadContext = [filePath: commentData.location, leftFileEnd: null, leftFileStart: null, rightFileEnd:null, rightFileStart: [line: 1, offset:0]]
		}
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repo.id}/pullRequests/${pullRequestId}/threads",
			query: ['api-version': '6.1-preview.1'],
			body: body
			)
		return result
	}
}
