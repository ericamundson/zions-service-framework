package com.zions.common.services.db

/**
 * All methods will produce a Map of db results  E.G.:
 * @author z091182
 *
 */
interface IDatabaseQueryService {
	
	/**
	 * @param sql
	 * @return Map of results
	 */
	def query(String sql);
	
	/**
	 * @return Map of result
	 */
	def nextPage();
	
	String pageUrl();
	
	String initialUrl()

}
