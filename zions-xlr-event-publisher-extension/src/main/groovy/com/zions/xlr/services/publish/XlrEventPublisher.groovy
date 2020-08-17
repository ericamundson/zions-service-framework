package com.zions.xlr.services.publish

import com.xebialabs.xlrelease.events.XLReleaseEventListener
import com.xebialabs.xlrelease.domain.events.ActivityLogEvent;
import com.xebialabs.xlrelease.events.AsyncSubscribe;

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.ApplicationContext

class XlrEventPublisher implements XLReleaseEventListener {
	static EventController eventController
	
	static {
		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		eventController = context.getBean(EventController)

	}
	
	@AsyncSubscribe
	public void notifyAnyChanges(ActivityLogEvent event) {
		eventController.forwardXLREvent(event)
	}
}
