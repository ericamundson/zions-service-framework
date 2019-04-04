package com.zions.common.services.cache

/**
 * Stores data and files to be cached to optimized interaction with target clouds systems.
 * 
 * @author z091182
 *
 */
interface ICacheManagementService {
	public static String WI_DATA = 'wiData'
	public static String RESULT_DATA = 'resultData'
	public static String RUN_DATA = 'runData'
	public static String PLAN_DATA = 'planData'
	public static String SUITE_DATA = 'suiteData'
	public static String CONFIGURATION_DATA = 'configurationData'
	
	/**
	 * Retrieve cached files.
	 * 
	 * @param id identity of object to be cached.
	 * @param type the brand of data being cached.
	 * @return cached data.
	 */
	def getFromCache(def id, String type);
	
	/**
	 * Retrieve cached files.
	 * 
	 * @param id identity of object to be cached.
	 * @param type the brand of data being cached.
	 * @return cached data.
	 */
	def getFromCache(def id, String module, String type);
	
	/**
	 * Save data to be cached
	 * 
	 * @param data - cache data
	 * @param id - types identifier
	 * @param type - type of cache object
	 * @return state of cache
	 */
	def saveToCache(def data, String id, String type);
	
	/**
	 * Cache a binary to be used for attachments
	 * 
	 * @param result - binary being cached
	 * @param name - file name to be used for cache file
	 * @param id - cache element identifier
	 * @return - currently returns a File
	 */
	def saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id);
	
	/**
	 * Clear cached
	 */
	void clear();
	
	void deleteById(String id);
	
	void deleteByType(String type);
	/**
	 * Check to see if element exists
	 */
	boolean exists(def key)
	
	/**
	 * Return all of type
	 * @param type
	 * @return
	 */
	def getAllOfType(String type)
}
