package com.zions.common.services.restart

/**
 * Interface to define processing checkpoints related to migration processing.
 * 
 * @author z091182
 *
 */
interface ICheckpointManagementService {
	/**
	 * Add a check point to list of processing.
	 * 
	 * @param phase - point in migration processing.
	 * @param pageUrl - query page url.
	 * @return
	 */
	def addCheckpoint(String phase, String pageUrl)
	
	
	/**
	 * Add a issue log entry to current checkpoint.
	 * 
	 * @param entry - log entry
	 * @return
	 */
	def addLogentry(String entry)
	
	
	/**
	 * 
	 * @return - current executing checkpoint
	 */
	Checkpoint getCurrentCheckpoint()
	
	/**
	 * Select a specific checkpoint 
	 * @param key - if key == 'last' it will move to last checkpoint, 
	 * 		if key == 'priorToLogEntries' then select checkpoint before any issues, 
	 * 		else key can be specific checkpoint entry.
	 * 
	 * @return - selected Checkpoint
	 */
	Checkpoint selectCheckpoint(String key)
	
	/**
	 * Clear all checkpoints
	 */
	void clear()
}
