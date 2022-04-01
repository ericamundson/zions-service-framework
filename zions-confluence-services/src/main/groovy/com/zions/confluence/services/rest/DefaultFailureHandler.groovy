package com.zions.confluence.services.rest

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
				if (resp.status == 400  || resp.status == 412)
					log.info("ADO Http ${resp.status} response:  ${errorMsg}")
				else
					log.error("ADO Http ${resp.status} response:  ${errorMsg}")
			}
			return resp
		}
	}
}
