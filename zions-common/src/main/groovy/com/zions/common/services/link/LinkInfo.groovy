package com.zions.common.services.link

import groovy.transform.Canonical

@Canonical
class LinkInfo {
	Date timeStamp
	
	String type  // This won’t always be a URL, but there will be a way to get type.
	
	String itemIdCurrent  //Current ID will be related to item being handled by @Links annotation.
	
	String itemIdRelated  // Related to item current item is linking.
	
	String moduleCurrent  // CCM,RQM,RRM

	String moduleRelated   // CCM,RQM,RRM

}
