package com.zions.vsts.services.tfs.rest

import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component
@Slf4j
class MicroserviceFailureHandler implements IFailureHandler {
	Closure getFailureHandler() {
		return { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				def logMsg = "ADO Http ${resp.status} response:  ${errorMsg}"
				if (resp.status == 400  || resp.status == 412)
					log.info(logMsg)
				else {
					log.error(logMsg)
					throw new Exception(logMsg)
				}
			}
			return resp
		}
	}
}
