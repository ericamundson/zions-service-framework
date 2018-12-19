package com.zions.common.services.cache

interface ICacheManagementService {
	public static String WI_DATA = 'wiData'
	public static String RESULT_DATA = 'resultData'
	public static String RUN_DATA = 'runData'
	public static String PLAN_DATA = 'planData'
	public static String SUITE_DATA = 'suiteData'
	
	def getFromCache(def id, String type);
	
	def saveToCache(def data, String id, String type);
	
	def saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id);
	
}
