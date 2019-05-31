package com.zions.common.services.restart

interface IQueryHandler {
	def getItems()
	String initialUrl()
	String getPageUrl()
	def nextPage()
	String getFilterName();
	boolean isModified(def item)
	
}
