package com.zions.common.services.restart

import com.zions.common.services.cache.ICacheManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles check points for processing restart start location management.
 * 
 * @author z091182
 *
 */
@Component
class CheckpointManagementService implements ICheckpointManagementService {
	
	@Value('${cache.location:}')
	String cacheLocation
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	
	private int idCounter = 0
	
	static private String CACHE_TYPE = 'checkpoint'
	
	private Checkpoint currentCheckpoint;
	
	public void resetIdCounter() {
		idCounter = 0;
	}

	@Override
	public Object addCheckpoint(String phase, String pageUrl) {
		Checkpoint cp = new Checkpoint()
		cp.checkpointId = idCounter
		cp.phase = phase
		cp.pageUrl = pageUrl
		currentCheckpoint = cp
		cacheManagementService.saveToCache(cp, "${idCounter}-${CACHE_TYPE}", CACHE_TYPE)
		idCounter++
		return null
	}

	@Override
	public Object addLogentry(String entry) {
		if (currentCheckpoint == null) return;
		currentCheckpoint.logEntries.add(entry)
		cacheManagementService.saveToCache(currentCheckpoint, "${idCounter-1}-${CACHE_TYPE}", CACHE_TYPE)
		return null
	}


	@Override
	public Checkpoint getCurrentCheckpoint() {
		// TODO Auto-generated method stub
		return currentCheckpoint
	}

	@Override
	public Checkpoint selectCheckpoint(String key) {
		if (key == 'last') {
			int i = 0
			while (true) {
				
				if (!cacheManagementService.exists("${i}-${CACHE_TYPE}")) {
					currentCheckpoint = loadCheckpoint(i-1)
					if (currentCheckpoint != null) {
						idCounter = currentCheckpoint.checkpointId
						idCounter++
					}
					return currentCheckpoint;
				}
				i++
			}
		} else if (key == 'priorToLogEntries') {
			int i = 0
			while (true) {
				if (!cacheManagementService.exists("${i}-${CACHE_TYPE}")) {
					currentCheckpoint = loadCheckpoint(i-1)
					if (currentCheckpoint != null) {
						idCounter = currentCheckpoint.checkpointId
						idCounter++
					}
					return currentCheckpoint;
				}  
				Checkpoint cp = loadCheckpoint(i)
				if (cp.logEntries.size()>0) {
					currentCheckpoint = loadCheckpoint(i-1)
					if (currentCheckpoint != null) {
						idCounter = currentCheckpoint.checkpointId
						idCounter++
					}
					return currentCheckpoint;

				}
				i++
			}

		}
		currentCheckpoint = loadCheckpoint(key)
		if (currentCheckpoint != null) {
			idCounter = currentCheckpoint.checkpointId
			idCounter++
		}
		return currentCheckpoint
	}
	
	void clear() {
		int counter = 0
		while (true) {
			if (cacheManagementService.exists("${i}-${CACHE_TYPE}")) {
				cacheManagementService.deleteById("${i}-${CACHE_TYPE}")
			} else {
				break;
			}
		}
		currentCheckpoint = null;
		idCounter = 0;
	}
	
	private Checkpoint loadCheckpoint(int id) {
		def cpMap = cacheManagementService.getFromCache("${id}-${CACHE_TYPE}", CACHE_TYPE)
		if (cpMap == null) {
			idCounter = 0;
			return null
		}
		return new Checkpoint(cpMap)

	}

	private Checkpoint loadCheckpoint(String key) {
		def cpMap = cacheManagementService.getFromCache(key, CACHE_TYPE)
		if (cpMap == null) {
			idCounter = 0;
			return null
		}
		return new Checkpoint(cpMap)

	}
	
	
}
