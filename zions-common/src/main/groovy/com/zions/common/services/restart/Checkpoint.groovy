package com.zions.common.services.restart

import groovy.transform.Canonical

/**
 * Bean to house restart checkpoint data.
 * 
 * @author z091182
 *
 */
@Canonical
class Checkpoint {
	/**
	 * Phase of operation of check point.
	 */
	String phase
	/**
	 * Query page url.
	 */
	String pageUrl
	/**
	 * Unique id of checkpoint
	 */
	int checkpointId
	/**
	 * Any ADO errors related to save of work item changes.
	 */
	def logEntries = []
	/**
	 * Time of checkpoint entry.
	 */
	String timeStamp
}
