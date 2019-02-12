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
	
	@Value('${item.filter:allFilter}')
	private String itemFilter
	
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
	 * participant "CheckpointManagementService:checkpointManagementService" as c
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
	 * this -> checkPointManagementService:addCheckpoint(phase, url)
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
			if (checkpoint == null || checkpoint.phase == phase) {
				remaining = true
				if (checkpoint != null) {
					while (url != checkpoint.url) {
						url = "${items.@href}"
						items = queryHandler.nextPage(url)
						
					}
				}
			} 
			if (remaining) {
				log.info("Starting ${phase}")
				while (true) {
				
					def inItems = filtered(items, itemFilter);
					checkpointManagementService.addCheckpoint(phase, url)
					
					// process integration logic
					closure(phase, inItems);
					url = queryHandler.pageUrl
					items = queryHandler.nextPage()
					if (items == null) break;
					
				}
				log.info("Ending ${phase}")
			}
		}
		return null
	}
	
	/**
	 * Filters top level queries items.
	 *
	 * @param items - ojgect of elements to be filtered Groovy object generation from XML rest result
	 * @param filter - Name of IFilter to use
	 * @return filtered result.
	 */
	def filtered(def items, String filter) {
		if (this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(items)
		}
		return items.entry.findAll { ti ->
			true
		}
	}


}
