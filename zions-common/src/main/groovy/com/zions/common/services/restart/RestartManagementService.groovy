package com.zions.common.services.restart

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.query.IFilter
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

/**
 * Handle restart on processing to ADO.
 * <p><b>Class Design:</b></p>
 * <img src="RestartManagmentService.png"/>
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
	
	/**
	 * There should be a query handler per phase that can be executed.
	 * Lookup for handler by name:  "${phase}QueryHandler" which should match class name
	 */
	@Autowired(required=false)
	Map<String, IQueryHandler> queryHandlers
	
	/**
	 * The list of phases to be run by command line action.
	 */
	@Value('${include.phases:}')
	public String includePhases

	/**
	 * Specifies phases that are updateable.
	 */
	@Value('${update.phases:}')
	public String[] updatePhases

	/**
	 * Work item filter map
	 */
	@Autowired(required=false)
	private Map<String, IFilter> filterMap;
	
	
	/**
	 * Service to store and retrieve page checkpoints.
	 */
	@Autowired
	ICheckpointManagementService checkpointManagementService
	
	/**
	 * selected.checkpoint Spring boot property has the following values
	 *  <ul>
	 *  <li>last - used to get last checkpoint page run</li>
	 *  <li>priorToLogEntries - If there are issues with any batch of work items log entries will get
	 *  added to checkpoint.  This checkpoint selection with checkpoint to any batch issues.</li>
	 *  <li> A specific checkpoint key can be selected.  E.G. 1-checkpoint</li>
	 *  <li>update - used to run against only source items that have updated since time of checkpoint</li>
	 *  </ul>
	 */
	@Value('${selected.checkpoint:none}')
	public String selectedCheckpoint
	

	/** 
	 * 
	 * Handle processing phases
	 * 
	 * <p><b>Flow:</b></p>
	 * <img src="RestartManagementService_processPhases_sequence_diagram.png"/>
	 * 
	 * @startuml RestartManagementService_processPhases_sequence_diagram.png
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
	 * this -> queryHandler: getPageUrl() : url
	 * this -> queryHandler: nextPage() : items
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
		if (selectedCheckpoint == 'none' || selectedCheckpoint == 'update') {
			checkpointManagementService.clear()
		}
		//checkpointManagementService.addCheckpoint('update', 'none')
		String[] phases = includePhases.split(',')
		// Move to checkpoint
		boolean remaining = false
		phases.each { String phase ->
			phase = phase.trim()
			String handlerName = "${phase}QueryHandler"
			IQueryHandler queryHandler = queryHandlers[handlerName]
			def items = queryHandler.getItems()
			String url = queryHandler.initialUrl();
			String filterName = queryHandler.filterName
			if (selectedCheckpoint == 'update') remaining = true
			if (checkpoint == null || checkpoint.phase == phase) {
				remaining = true
				if (checkpoint != null) {
					while (url != checkpoint.pageUrl) {
						//log.debug("Skipping to next page to catch up with checkpoint")
						url = queryHandler.getPageUrl()
						items = queryHandler.nextPage()
					}
				}
			} 
			if (remaining) {
				log.info("Starting ${phase}")
				while (true) {
					//log.debug("top of phase loop inside restartmanager")
					def inItems = filtered(items, filterName);
					if (selectedCheckpoint == 'update' && updatePhases.contains(phase)) {
						inItems = filterForUpdate(inItems, checkpoint, queryHandler)
					}
					//log.debug("Adding checkpoint for phase: ${phase} | url: ${url}")
					checkpointManagementService.addCheckpoint(phase, url)
					
					// process integration logic
					//log.debug("going to RestartManagementService::processPhases closure with item count: ${inItems.size()}")
					if (inItems.size() > 0) {
						closure(phase, inItems);
					}
					//log.debug("Setting url and nextPage items for phase loop")
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
	 * filter page items for phase processing.
	 * 
	 * @param items - input page items
	 * @param cp - currently not used.
	 * @param qHandler - current phase handler
	 * @return filtered items
	 */
	def filterForUpdate(def items, Checkpoint cp, IQueryHandler qHandler) {
		//Date startDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", cp.timeStamp)
		
		def outItems = items.findAll { item ->
			boolean flag = qHandler.isModified(item)
			flag
		}
		return outItems;
	}
	
	/**
	 * Filters top level queries items.
	 *
	 * @param items - object of elements to be filtered Groovy object generation from XML rest result
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
