package com.zions.pipeline.services.feedback

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.pipeline.services.mixins.FeedbackTrait

@Component
class LogCallbackHandler implements FeedbackTrait {
	String pipelineId
	
	public LogCallbackHandler() {
		
	}
	
	
	Closure failureCallback = { resp ->
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
}
