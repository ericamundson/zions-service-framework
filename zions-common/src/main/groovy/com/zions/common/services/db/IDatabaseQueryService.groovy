package com.zions.common.services.db

/**
 * All methods will produce a Map of db results  E.G.:
 * @author z091182
 *
 */
interface IDatabaseQueryService {
	
	/**
	 * Initial query start and returns initial page results.
	 * 
	 * @param sql
	 * @return Map of results
	 */
	def query(String sql);
	
	/**
	 * Initial query start with input parms map and returns initial page results.
	 * 
	 * @param sql - statement
	 * @param parms - map of name: value pairs.
	 * @return result map.
	 */
	def query(String sql, def parms);
	
	/**
	 * @return Map of result
	 */
	def nextPage();
	
	/**
	 * @return a page's url
	 */
	String pageUrl();
	
	/**
	 * @return initial page url.
	 */
	String initialUrl()

}
