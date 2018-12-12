package com.zions.common.services.cache

interface ICacheManagementService {
	public static String WI_DATA = 'wiData'
	public static String RESULT_DATA = 'resultData'
	public static String RUN_DATA = 'runData'

	def getFromCache(def id, String type);
	
	def saveToCache(def data, String id, String type);
	
	def saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id);
	
}
