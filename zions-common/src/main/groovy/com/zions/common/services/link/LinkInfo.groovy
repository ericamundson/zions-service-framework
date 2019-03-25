package com.zions.common.services.link

import groovy.transform.Canonical

@Canonical
class LinkInfo {
	Date timeStamp  //Used to ensure link cache is current.
	
	String type  // This won’t always be a URL, but there will be a way to get type.
	
	String itemIdCurrent  //Current ID will be related to item being handled by @Links annotation.
	
	String itemIdRelated  // Related ID/URL to related item.  If url, item will need to be retrieve for id to caching.
	
	String moduleCurrent  // CCM,QM,RM

	String moduleRelated   // CCM,QM,RM

}
