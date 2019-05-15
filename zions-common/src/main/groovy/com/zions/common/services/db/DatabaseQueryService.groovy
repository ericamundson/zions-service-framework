package com.zions.common.services.db

import groovy.sql.Sql
import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Concrete Database query service.
 * 
 * @author z091182
 *
 */
@Slf4j
@Component
class DatabaseQueryService implements IDatabaseQueryService {
	@Value('${db.url:}')
	String dbUrl
	
	@Value('${db.user:}')
	String dbUser
	
	@Value('${db.password:}')
	String dbPassword
	
	@Value('${db.driver:oracle.jdbc.datasource.OracleDataSource}')
	String dbDriver

	@Value('${page.size:100}')
	int pageSize

	
	Sql sql = null
	
	def columnNames = []
	
	def index = 1
	
	String select
	
	def init() {
		if (sql) return
		sql = groovy.sql.Sql.newInstance(dbUrl, dbUser, dbPassword, dbDriver)
	}
	
	def metaCB = { metaData -> 
		int c = metaData.columnCount 
		this.columnNames.clear()
		
		for (int i = 0; i<c; i++) {      
			this.columnNames.add(metaData.getColumnLabel(i+1))
		}
	}
	
	@Override
	public def query(String select) {
		init()
		this.select = select
		index = 1
		def page = []
		sql.eachRow(this.select,metaCB, index, pageSize) { row ->
			def rowm = [:]
			columnNames.each { name ->
				String oname = "${name}".toLowerCase()
				rowm[oname] = row."${name}"
			}
			page.add(rowm)
		}
		return page;
	}

	@Override
	public def nextPage() {
		index += pageSize
		def page = []
		sql.eachRow(this.select, index, pageSize) { row ->
			def rowm = [:]
			columnNames.each { name ->
				String oname = "${name}".toLowerCase()
				rowm[oname] = row."${name}"
			}
			page.add(rowm)
		}
		if (page.size() == 0) return null
		return page;
	}
	
	public String pageUrl() {
		return "${select}/${index+pageSize}/${pageSize}"
	}

	@Override
	public String initialUrl() {
		// TODO Auto-generated method stub
		return "${select}/1/${pageSize}"
	}
}

