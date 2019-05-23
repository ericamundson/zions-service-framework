package com.zions.common.services.db

import groovy.sql.Sql

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Concrete Database query service.
 * 
 * @author z091182
 *
 */
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
	
	def parms
	
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
	
	public def query(String select, def parms = null) {
		init()
		this.select = select
		this.parms = parms
		index = 1
		def page = []
		if (parms) {
			sql.eachRow(this.select,parms,metaCB, index, pageSize) { row ->
				def rowm = [:]
				columnNames.each { name ->
					String oname = "${name}".toLowerCase()
					rowm[oname] = row."${name}"
				}
				page.add(rowm)
			}
		} else {
			sql.eachRow(this.select, metaCB, index, pageSize) { row ->
				def rowm = [:]
				columnNames.each { name ->
					String oname = "${name}".toLowerCase()
					rowm[oname] = row."${name}"
				}
				page.add(rowm)
			}

		}
		return page;
	}

	@Override
	public def nextPage() {
		def page = []
		if (parms) {
			sql.eachRow(this.select,parms,metaCB, index, pageSize) { row ->
				def rowm = [:]
				columnNames.each { name ->
					String oname = "${name}".toLowerCase()
					rowm[oname] = row."${name}"
				}
				page.add(rowm)
			}
		} else {
			sql.eachRow(this.select, metaCB, index, pageSize) { row ->
				def rowm = [:]
				columnNames.each { name ->
					String oname = "${name}".toLowerCase()
					rowm[oname] = row."${name}"
				}
				page.add(rowm)
			}

		}
		if (page.size() == 0) return null
		return page;
	}
	
	//yeah, in theory this should be current page
	//and nextPage should jump up the count, but it breaks the other way
	//due to caching not incrementing pages, URI never advances, checkpoints
	//get messed up
	public String pageUrl() {
		index += pageSize
		return "${select}/${index+pageSize}/${pageSize}"
	}

	@Override
	public String initialUrl() {
		// TODO Auto-generated method stub
		return "${select}/1/${pageSize}"
	}
	
	//might not be best practice here for interface implementation
	//but this is here because: a restarted process never calls
	//this.query function (as it pulls the query data from cache)
	//thus, the select string is never set and upon restart the uri
	//are always returned as 'null/x/size'
	//since we know initialUrl is always invoked by restartmanager,
	//we just have the controller BaseDatabaseQueryHandler (or whatever)
	//set the initial URI this way regardless of query being invoked
	//tl;dr: caching system results in null this.select on checkpoint restart
	public String initialUrl(String query) {
		this.select = query
		return initialUrl()
	}
}

