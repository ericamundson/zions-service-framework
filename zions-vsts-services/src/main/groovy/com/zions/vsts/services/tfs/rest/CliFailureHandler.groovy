package com.zions.vsts.services.tfs.rest

import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component
@Slf4j
class CliFailureHandler implements IFailureHandler {
	Closure getFailureHandler() {
		return { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				def logMsg = "ADO Http ${resp.status} response:  ${errorMsg}"
				// Supppress 404 messages, since these are not really errors and code can handle by checking for empty response
				if (resp.status != 404)
				{
					log.error(logMsg)
				}
			}
			return resp
		}
	}
}
