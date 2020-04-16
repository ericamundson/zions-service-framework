package com.zions.vsts.services.work.calculations

import com.zions.vsts.services.work.WorkManagementService
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handle parent activation.
 * 
 * @author z070187
 *
 */
@Component
@Slf4j
class ParentActivationManagementService {
	
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	public ParentActivationManagementService() {
		
	}
	//getting parent data
	def performParentActivation(String id, String childState, String project) {
		def wi = workManagementService.getWorkItem(collection, project, id)
		
		def parent = workManagementService.getParent(collection,project, wi)
		
		if (parent) {
			String pid = "${parent.id}"
			//rollup(pid, true, project)
			activateParent(pid, true, project)
		}
	}
	/**
	 * Activate a specific work item parent.
	 * 
	 * @param id - id of work item child.
	 * @param childState - flag to handle parent activation.  Future for Task changes.
	 * @param project - related project to work items.
	 */
	
	//will pull id, childState and project from parentActivationMicroService
	void activateParent(String pwi, childState, String project) {
	
		//do I need to define a variable for parent state?
		
	//String pState = workManagementService.getState(collection, project, pwi)
		String pState = "${pwi.fields.'System.State'}"
		
		//enclose the save in an IF under conditions to save the parent
		//if old state is new and new state is active set parent state to active
		//if (childState == 'Closed' || 'Active' and the Parent State isn't already active
		//set parent state to Active
		if ((childState == 'Closed' || childState == 'Active') && pState != 'Active') {
			
			String pid = "${pwi.id}"
			//private save(pid, wi, 'Active', project)
			save(pid, pwi, pState, project)
		}
	}
	
	//creating json payload for update attribute
	private save(String id, def wi, String newState, String project) {
		def data = []
		boolean changed = false;
		//add Cstate to management service
		String cState = wi.fields['System.State'] 
		
		if (cState != newState) {
			def t = [op: 'test', path: '/rev', value: wi.rev]
			data.add(t)
		
			def e = [op: 'add', path: '/fields/System.State', value: 'Active']
			data.add(e)
			changed = true;
		}
	  
   
				
		if (changed) {
			workManagementService.updateWorkItem(collection, project, id, data)
		}
	}
	
}	
	
