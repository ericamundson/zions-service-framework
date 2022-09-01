package com.zions.common.services.slf4j;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.mongodb.MongoClientURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

@Component
public class MongoAppender extends UnsynchronizedAppenderBase<ILoggingEvent> implements ApplicationContextAware {
    private static MongoTemplate mongoTemplate;
    private String collectionName;
	
	@Value('${override.collectionName:#{null}}')
	private String overrideCollectionName

    protected void append(ILoggingEvent event) {
        if (!started || mongoTemplate == null) {
            return;
        }
		
		String cName = collectionName
		if (overrideCollectionName != null) {
			cName = overrideCollectionName
		}

        LogEntity log = new LogEntity();
        log.threadName = event.getThreadName();
        log.level = event.getLevel().levelStr;
        log.formattedMessage = event.getFormattedMessage();
        log.loggerName = event.getLoggerName();
        log.timestamp = event.getTimeStamp();
        mongoTemplate.save(log, collectionName);
    }

    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext.getAutowireCapableBeanFactory().getBean(MongoTemplate.class) != null) {
            mongoTemplate = applicationContext.getAutowireCapableBeanFactory().getBean(MongoTemplate.class);
            LoggerFactory.getLogger(this.getClass()).info("[ApplicationContext] Autowire MongoTemplate, MongoAppender is ready.");
        }
    }


    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}