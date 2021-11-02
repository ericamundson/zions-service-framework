package com.zions.vsts.pubsub.dr

import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component('git.pullrequest.merged')
@Slf4j
class GitPullrequestMergedCheck implements DrTestRequired {
	boolean requiresSendToDR(def adoEvent) {
		String targetRef = "${adoEvent.resource.targetRefName}"
		
		//log.info("Inside GitPullRequestMergedCheck:requiresSendToDR targetRefName: ${targetRef}")
		if (targetRef.toLowerCase().startsWith('refs/heads/dr/')) return true
		return false
	}
	
	String getType() {
		return 'git.pullrequest.merged'
	}
}
