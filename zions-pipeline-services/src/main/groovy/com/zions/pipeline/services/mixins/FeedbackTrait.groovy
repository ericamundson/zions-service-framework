package com.zions.pipeline.services.mixins
import org.springframework.beans.factory.annotation.Autowired
import com.zions.pipeline.services.feedback.FeedbackService

trait FeedbackTrait {
	@Autowired
	FeedbackService feedbackService
	
	void logContextStart(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logContextStart(pid, message)
		}
	}
	
	void logContextComplete(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logContextComplete(pid, message)
		}
	}

	void logInfo(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logInfo(pid, message)
		}
	}
	void logWarn(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logWarn(pid, message)
		}
	}
	void logError(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logError(pid, message)
		}
	}
	void logFailed(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logFailed(pid, message)
		}
	}
	void logCompleted(String pid, String message) {
		if (pid && pid.length() > 0) {
			feedbackService.logCompleted(pid, message)
		}
	}
}
