package com.zions.common.services.restart

/**
 * Implemented by specific integration to external system being migrated to handle queries 
 * and query paging.
 * 
 * @author z091182
 *
 */
interface IQueryHandler {
	/**
	 * Initial query and page
	 * @return initial page
	 */
	def getItems()
	/**
	 * Inital page url
	 * @return url
	 */
	String initialUrl()
	/**
	 * @return paged url
	 */
	String getPageUrl()
	/**
	 * @return move to next page data
	 */
	def nextPage()
	/**
	 * @return work item filter name
	 */
	String getFilterName();
	/**
	 * Used to determine if item should be update if it exists in ADO.
	 * @param item - item to check
	 * @return true if ADO item needs updated.
	 */
	boolean isModified(def item)
	
}
