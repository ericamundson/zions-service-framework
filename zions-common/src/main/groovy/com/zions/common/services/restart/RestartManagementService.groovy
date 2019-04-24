package com.zions.common.services.restart

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter
import groovy.util.logging.Slf4j

/**
 * Handle restart on processing to ADO.
 * 
 * @author z091182
 *
 *@startuml RestartManagmentService.png
 *class RestartManagementService {
 *+ def processPhases(Closure closure)
 *}
 *class Map<String,IQueryHandler> {
 *}
 *IRestartManagementService <|.. RestartManagementService
 *RestartManagementService --> Map: queryHandlers
 *ICheckpointManagementService <|.. CheckpointManagementService
 *RestartManagementService --> ICheckpointManagementService: checkpointManagementService
 *@enduml
 */
@Component
@Slf4j
class RestartManagementService implements IRestartManagementService {
	
	@Autowired(required=false)
	private Map<String, IQueryHandler> queryHandlers
	
	@Value('${include.phases:}')
	private String includePhases
	
	@Autowired(required=false)
	private Map<String, IFilter> filterMap;
	
	
	@Autowired
	ICheckpointManagementService checkpointManagementService
	
	@Value('${selected.checkpoint:none}')
	private String selectedCheckpoint
	

	/** 
	 * 
	 * Handle processing phases
	 * 
	 * @startuml
	 * participant "RestartManagementService:this" as this
	 * participant "Closure:closure" as closure
	 * participant "CheckpointManagementService:checkpointManagementService" as checkpointManagementService
	 * participant "String[]:phases" as phases
	 * 
	 * this -> checkpointManagementService: selectCheckpoint(this.selectedCheckpoint):checkpoint
	 * this -> this: includedPhases.split(',') : phases
	 * loop each phase
	 * this -> "Map<string,IQueryHander>:queryHandlers" as queryHandlers: get(handlerName) : queryhandler
	 * this -> "IQueryHandler:queryHandler" as queryHandler: getItems() : items
	 * this -> "boolean:remaining" as remaining: set false
	 * alt checkpoint == null || checkpoint.phase == phase
	 * this -> remaining: set true
	 * alt checkpoint != null
	 * loop checkpoint.url != url
	 * this -> queryHandler: nextPage(url)
	 * end
	 * end
	 * end
	 * alt remaining == true
	 * loop true
	 * this -> this: filtered(filterName)
	 * alt selectedCheckpoint == 'update'
	 * this -> this: filterForUpdate(items) : items
	 * end
	 * this -> checkpointManagementService:addCheckpoint(phase, url)
	 * this -> closure: call(phase, items)
	 * this -> queryHandler: nextPage(url) : items
	 * alt items == null
	 * this -> this: break;
	 * end
	 * end
	 * end
	 * @enduml
	 * 	 
	 */
	public Object processPhases(Closure closure) {
		Checkpoint checkpoint = checkpointManagementService.selectCheckpoint(selectedCheckpoint);
		String[] phases = includePhases.split(',')
		// Move to checkpoint
		boolean remaining = false
		phases.each { String phase ->
			String handlerName = "${phase}QueryHandler"
			IQueryHandler queryHandler = queryHandlers[handlerName]
			def items = queryHandler.getItems()
			String url = queryHandler.initialUrl();
			String filterName = queryHandler.filterName
			if (checkpoint == null || checkpoint.phase == phase) {
				remaining = true
				if (checkpoint != null) {
					while (url != checkpoint.pageUrl) {
						log.debug("Skipping to next page to catch up with checkpoint")
						url = queryHandler.getPageUrl()
						items = queryHandler.nextPage()
					}
				}
			} 
			if (remaining) {
				log.info("Starting ${phase}")
				while (true) {
					log.debug("top of phase loop inside restartmanager")
					def inItems = filtered(items, filterName);
					if (selectedCheckpoint == 'update') {
						inItems = filterForUpdate(inItems, checkpoint)
					}
					log.debug("Adding checkpoint for phase: ${phase} | url: ${url}")
					checkpointManagementService.addCheckpoint(phase, url)
					
					// process integration logic
					log.debug("going to wrapped worker with item count: ${inItems.size()}")
					closure(phase, inItems);
					log.debug("Setting url and nextPage items for phase loop")
					url = queryHandler.pageUrl
					items = queryHandler.nextPage()
					if (items == null) break;
					
				}
				log.info("Ending ${phase}")
			}
		}
		checkpointManagementService.addCheckpoint('update', 'none')
		return null
	}
	
	def filterForUpdate(def items, Checkpoint cp, IQueryHandler qHandler) {
		Date startDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", cp.timeStamp)
		
		def outItems = items.findAll { item ->
			Date iDate = qHandler.modifiedDate(item)
			iDate >= startDate
		}
		return outItems;
	}
	
	/**
	 * Filters top level queries items.
	 *
	 * @param items - ojgect of elements to be filtered Groovy object generation from XML rest result
	 * @param filter - Name of IFilter to use
	 * @return filtered result.
	 */
	def filtered(def items, String filter) {
		if (this.filterMap != null && this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(items)
		}
		return items.entry.findAll { ti ->
			true
		}
	}


}
