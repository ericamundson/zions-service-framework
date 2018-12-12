package com.zions.common.services.cache

interface ICacheManagementService {
	def getFromCache(def id, String type);
	
	def saveToCache(def data, String id, String type);
	
	def saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id);
	
}
