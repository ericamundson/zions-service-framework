package com.zions.vsts.pubsub.dr

import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component('git.push')
@Slf4j
class GitPushCheck implements DrTestRequired {
	boolean requiresSendToDR(def adoEvent) {
	    def changeSet = adoEvent.resource;
		for (def update in changeSet.refUpdates) {
			String name = update.name
			if (name && name.toLowerCase().startsWith('refs/heads/dr/')) {
				return true
			}
		}
		return false
	}
	
	String getType() {
		return 'git.push'
	}
}
