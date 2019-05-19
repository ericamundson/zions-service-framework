package com.zions.common.services.db

import static org.junit.Assert.*

import groovy.sql.Sql
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
@ContextConfiguration(classes=[DatabaseQueryServiceSpecConfig])
class DatabaseQueryServiceSpec extends Specification {

	@Autowired
	IDatabaseQueryService databaseQueryService

	def 'Run db test with paging'() {
		setup: 'Setup data'
		setupDbData()
		
		when:  'call query and paging calls'
		boolean flag = true
		try {
			println 'JVM Languages:'
			def page = databaseQueryService.query('select * from languages')
			String iUrl = databaseQueryService.initialUrl()
			while (true) {
				page.each { item ->
					println "	Id: ${item.id}, Name: ${item.name}"
				}
				println '	end page'
				iUrl = databaseQueryService.pageUrl()
				page = databaseQueryService.nextPage()
				if (!page) break;
			}
			println 'With parm JVM Languages:'
			page = databaseQueryService.query("select * from languages where name = :name", [name: 'Groovy'])
			iUrl = databaseQueryService.initialUrl()
			while (true) {
				page.each { item ->
					println "	Id: ${item.id}, Name: ${item.name}"
				}
				println '	end page'
				iUrl = databaseQueryService.pageUrl()
				page = databaseQueryService.nextPage()
				if (!page) break;
			}
		} catch (e) {
			flag = false
		}
		then:
		flag
		
		cleanup:
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
@PropertySource("classpath:test.properties")
class DatabaseQueryServiceSpecConfig {
	
	@Bean
	IDatabaseQueryService databaseQueryService() {
		return new DatabaseQueryService()
	}
	
}