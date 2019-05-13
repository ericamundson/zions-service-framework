package com.zions.common.services.db

import groovy.sql.Sql
import groovy.util.logging.Slf4j

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData

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
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
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
		index += pageSize
		return "${select}/${index}/${pageSize}"
	}

	@Override
	public String initialUrl() {
		// TODO Auto-generated method stub
		return "${select}/1/${pageSize}"
	}
	
	def flushQueries() {
		Date ts = new Date()
		cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
		int pageCount = 0
		def currentItems
		def iUrl
//		try {
		init()
		new CacheInterceptor() {}.provideCaching(this, "${pageCount}", ts, DataWarehouseQueryData) {
			currentItems = this.query(QueryString())
		}
		while (true) {
			iUrl = this.pageUrl()
			pageCount++
			new CacheInterceptor() {}.provideCaching(this, "${pageCount}", ts, DataWarehouseQueryData) {
				currentItems = this.nextPage()
			}
			if(!currentItems) break;
		}
	}
	
	public String QueryString() {
		if (select) return select
		return '''SELECT T1.REFERENCE_ID as reference_id,
  T1.URL as about,
  T1.Primary_Text as text
FROM RIDW.VW_REQUIREMENT T1
LEFT OUTER JOIN RICALM.VW_RQRMENT_ENUMERATION T2
ON T2.REQUIREMENT_ID=T1.REQUIREMENT_ID AND T2.NAME='Release'
WHERE T1.PROJECT_ID = 19  AND
(  T1.REQUIREMENT_TYPE NOT IN ( 'Change Request','Actor','Use Case','User Story','Spec Proxy','Function Point','Process Inventory','Term','Use Case Diagram' ) AND
  T2.LITERAL_NAME = 'Deposits'  AND
  LENGTH(T1.URL) = 65 AND
  T1.REC_DATETIME > TO_DATE('05/01/2014','mm/dd/yyyy')
) AND
T1.ISSOFTDELETED = 0 AND
(T1.REQUIREMENT_ID <> -1 AND T1.REQUIREMENT_ID IS NOT NULL)
'''
	}

}

@Slf4j
class DataWarehouseQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		log.debug("DWQueryData serializing result doData")
		data = new JsonOutput().toJson(result)
	}
	
	def dataValue() {
		log.debug("DWQueryData returning serialized result dataValue")
		return new JsonSlurper().parseText(data)
	}

}
