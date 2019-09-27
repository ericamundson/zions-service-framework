package com.zions.common.services.db

import static org.junit.Assert.*

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification


@ContextConfiguration(classes=[DatabaseQueryServiceSpecConfig])
@Slf4j
class DatabaseQueryServiceSpec extends Specification {

	@Autowired
	IDatabaseQueryService databaseQueryService

	def 'Run db test with paging'() {
		setup: s_ 'Setup data'
		setupDbData()
		
		when: w_  'call query and paging calls'
		boolean flag = true
		log.info 'JVM Languages:'
		def page = databaseQueryService.query('select * from languages')
		String iUrl = databaseQueryService.initialUrl()
		log.info "   pageUrl: ${iUrl}" 
		while (true) {
			page.each { item ->
				log.info "	Id: ${item.id}, Name: ${item.name}"
			}
			log.info '	end page'
			iUrl = databaseQueryService.pageUrl()
			log.info "   pageUrl: ${iUrl}" 
			page = databaseQueryService.nextPage()
			if (!page) break;
		}
		then: t_ 'validate query with no parms'
		iUrl == 'select * from languages/11/5'
		
		when: w_'Run query with parms'
		log.info 'With parm JVM Languages:'
		page = databaseQueryService.query("select * from languages where name = :name", [name: 'Groovy'])
		iUrl = databaseQueryService.initialUrl()
		log.info "   pageUrl: ${iUrl}" 
		while (true) {
			page.each { item ->
				log.info "	Id: ${item.id}, Name: ${item.name}"
			}
			log.info '	end page'
			iUrl = databaseQueryService.pageUrl()
			log.info "   pageUrl: ${iUrl}" 
			page = databaseQueryService.nextPage()
			if (!page) break;
		}
		then: t_ 'validate query with parms'
		iUrl == 'select * from languages where name = :name/6/5'
		
		cleanup: c_ 'Cleanup db data'
		cleanDbData()
	}
	
	def setupDbData() {
		databaseQueryService.init()
		Sql sql = databaseQueryService.sql
		sql.execute 'drop table if exists languages'
		sql.execute '''
    create table languages(
        id integer not null auto_increment,
        name varchar(50) not null,
        primary key(id)
    )
'''
		 
		['Groovy', 'Java', 'Kotlin', 'JRuby', 'Clojure', 'Jython', 'Pascal', 'Cobol', 'Smalltalk', 'Visual Basic (Jabaco)'].each {
			sql.execute "insert into languages(name) values($it)"
		}
	}
	
	def cleanDbData() {
		Sql sql = databaseQueryService.sql
		
		sql.execute "drop table languages"
		sql.close()
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.logging"])
@PropertySource("classpath:test.properties")
class DatabaseQueryServiceSpecConfig {
	
	@Bean
	IDatabaseQueryService databaseQueryService() {
		return new DatabaseQueryService()
	}
	
}