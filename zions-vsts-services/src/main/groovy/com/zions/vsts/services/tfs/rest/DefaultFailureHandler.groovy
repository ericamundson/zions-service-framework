package com.zions.vsts.services.tfs.rest

import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component
@Slf4j
class DefaultFailureHandler implements IFailureHandler {
	Closure getFailureHandler() {
		return { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				log.error("ADO Http error response:  ${errorMsg}")
			}
			return resp
		}
	}
}
