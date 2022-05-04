package com.zions.common.services.slf4j

import org.springframework.data.mongodb.core.mapping.Document

import groovy.transform.Canonical

@Document
@Canonical
class LogEntity {
        String threadName;
        String level;
        String formattedMessage;
        String loggerName;
        Long timestamp;
		
		String toString() {
			String stimestamp = new Date(timestamp).format('yyyy-MM-dd HH:mm:ss')
			return "${stimestamp} ${level} ${loggerName} $formattedMessage"
		}
}
 