package com.zions.pipeline.services.mixins
import org.springframework.beans.factory.annotation.Autowired
import com.zions.pipeline.services.feedback.FeedbackService
import java.util.regex.Pattern

trait FeedbackTrait {
	@Autowired
	FeedbackService feedbackService
	
	String sanitizeMessage(String message, def checks) {
		if (checks instanceof List) {
			checks.each { check ->
				def pattern = check.pattern
				def replacement = check.replacement
				message = message.replaceAll(pattern, replacement)
			}
		}
		return message
	}
	
	void logContextStart(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logContextStart(pid, message)
		}
	}
	
	void logContextComplete(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logContextComplete(pid, message)
		}
	}

	void logInfo(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logInfo(pid, message)
		}
	}
	void logWarn(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logWarn(pid, message)
		}
	}
	void logError(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logError(pid, message)
		}
	}
	void logFailed(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logFailed(pid, message)
		}
	}
	void logCompleted(String pid, String message, def check = null) {
		if (check) {
			message = sanitizeMessage(message, check)
		}
		if (pid && pid.length() > 0) {
			feedbackService.logCompleted(pid, message)
		}
	}
}
