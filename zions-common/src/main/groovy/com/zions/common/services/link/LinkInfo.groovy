package com.zions.common.services.link

import com.zions.common.services.cacheaspect.CacheRequired

import groovy.transform.Canonical

/**
 * Data required for work item linking.
 * 
 * @author z091182
 *
 */
@Canonical
class LinkInfo implements CacheRequired {
	
	String type  // This won’t always be a URL, but there will be a way to get type.
	
	String itemIdCurrent  //Current ID will be related to item being handled by @Links annotation.
	
	String itemIdRelated  // Related ID/URL to related item.  If url, item will need to be retrieve for id to caching.
	
	String moduleCurrent  // CCM,QM,RM

	String moduleRelated   // CCM,QM,RM

}
