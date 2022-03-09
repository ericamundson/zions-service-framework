package com.zions.xlr.services.publish

import com.xebialabs.xlrelease.events.XLReleaseEventListener
import com.xebialabs.xlrelease.domain.events.ActivityLogEvent;
import com.xebialabs.xlrelease.events.AsyncSubscribe;

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.ApplicationContext

class XlrEventPublisher implements XLReleaseEventListener {
	static EventController eventController
	
	static {
		String hostName
		try {
			hostName = InetAddress.getLocalHost().getHostName().toLowerCase()
		 } catch(Exception ex) {
			println("Unable to get Hostname")
		}
		def profileMap = [utlxa221: 'k8sprod', utlxa220: 'test', drutlxa221: 'dr', utmsdev0527: 'dev']
		def mqHost = [utlxa221: 'utlxvpi00286', utlxa220: '172.20.104.15', drutlxa221: 'utmvpi0144', utmsdev0527: '172.20.104.15']
		String profile = 'k8sprod'
		if (profileMap[hostName] ){
			profile = profileMap[hostName]
		}
		String host = 'utlxvpi00286'
		if (mqHost[hostName]) {
			host = mqHost[hostName]
		}
		System.setProperty('spring.profiles.active', profile)
		System.setProperty('spring.rabbitmq.host', host)
		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		eventController = context.getBean(EventController)

	}
	
	@AsyncSubscribe
	public void notifyAnyChanges(ActivityLogEvent event) {
		eventController.forwardXLREvent(event)
	}
}
