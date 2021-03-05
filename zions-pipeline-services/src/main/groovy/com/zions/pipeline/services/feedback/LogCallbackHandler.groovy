package com.zions.pipeline.services.feedback

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.vsts.services.tfs.rest.IFailureHandler

@Component
class LogCallbackHandler implements IFailureHandler, FeedbackTrait {
	String pipelineId
	
	public LogCallbackHandler() {
		
	}
	
	Closure failureHandlerImpl = { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				if (pipelineId) {
					logWarn(pipelineId, "ADO Http error response:  ${errorMsg}")
				}
			}
			return resp
	}

	Closure getFailureHandler() {
		return failureHandlerImpl
	}
	
	
}
