package com.zions.vsts.services.work.calculations

import com.zions.vsts.services.work.WorkManagementService
import groovy.transform.Canonical
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handle work rollup calculations.
 * 
 * @author z091182
 *
 */
@Component
class RollupManagementService {
	
	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	public RollupManagementService() {
		
	}
	

	/**
	 * Rollup a specfic work item's work data.
	 * 
	 * @param id - id of work item to rollup.
	 * @param parentRollup - flag to handle parent rollup.  Future for Task changes.
	 * @param project - related project to work items.
	 */
	void rollup(String id, boolean parentRollup, String project) {
		def wi = workManagementService.getWorkItem(collection, project, id)
		String pCat = workManagementService.getCategory(collection, project, wi)
		
		if (pCat != 'Feature Category' && pCat != 'Requirement Category' ) return
		String pid = "${wi.id}"
		List<RollupInfo> childData = getChildData(pid, pCat, project)
		
		int estimate = 0
		int remaining = 0
		int completed = 0
		childData.each { cd -> 
			estimate += cd.estimate
			remaining += cd.remaining
			completed += cd.completed
		}
		save(pid, wi, estimate, remaining, completed, project)
	}
	
	private save(String id, def wi, int estimate, int remaining, int completed, String project) {
		def data = []
		boolean changed = false;
		int cEstimate = wi.fields['Custom.RollupEstimate'] != null ? wi.fields['Custom.RollupEstimate'] : -1
		int cRemaining = wi.fields['Custom.RollupRemaining'] != null ? wi.fields['Custom.RollupRemaining'] : -1
		int cCompleted = wi.fields['Custom.RollupCompleted'] != null ? wi.fields['Custom.RollupCompleted'] : -1
		def t = [op: 'test', path: '/rev', value: wi.rev]
		data.add(t)
		if (estimate != cEstimate) {
			def e = [op: 'add', path: '/fields/Custom.RollupEstimate', value: estimate]
			data.add(e)
			changed = true;
		}
		if (remaining != cRemaining) {
			def r = [op: 'add', path: '/fields/Custom.RollupRemaining', value: remaining]
			data.add(r)
			changed = true;
		}
		if (completed != cCompleted) {
			def c = [op: 'add', path: '/fields/Custom.RollupCompleted', value: completed]
			data.add(c)
			changed = true;
		}
		if (changed) {
			workManagementService.updateWorkItem(collection, project, id, data)
		}
	}
	
	
	private List<RollupInfo> getChildData(String parentId, String parentCategory, String project) {
		def children = workManagementService.getChildren(collection, project, parentId)
		List<RollupInfo> outInfo = []
		children.each { wi ->
			String cat = workManagementService.getCategory(collection, project, wi)
			String type = "${wi.fields['System.WorkItemType']}"
			if (type == 'Task' && parentCategory == 'Requirement Category') {
				int estimate = 0
				if (wi.fields['Microsoft.VSTS.Scheduling.OriginalEstimate']) {
					estimate = wi.fields['Microsoft.VSTS.Scheduling.OriginalEstimate']
				}
				int remaining = 0;
				if (wi.fields['Microsoft.VSTS.Scheduling.RemainingWork']) {
					remaining = wi.fields['Microsoft.VSTS.Scheduling.RemainingWork']
				}
				int completed = 0;
				if (wi.fields['Microsoft.VSTS.Scheduling.CompletedWork']) {
					completed = wi.fields['Microsoft.VSTS.Scheduling.CompletedWork']
				}

				RollupInfo rollupInfo = new RollupInfo([estimate:estimate, remaining:remaining, completed:completed])
				outInfo.add(rollupInfo)
			} else if (cat == 'Requirement Category' && parentCategory == 'Feature Category') {
				String cid = "${wi.id}"
				this.rollup(cid, false, project)
				int estimate = 0
				def uwi = workManagementService.getWorkItem(collection, project, cid)
				if (uwi.fields['Custom.RollupEstimate']) {
					estimate = uwi.fields['Custom.RollupEstimate']
				}
				int remaining = 0;
				if (uwi.fields['Custom.RollupRemaining']) {
					remaining = uwi.fields['Custom.RollupRemaining']
				}
				int completed = 0;
				if (uwi.fields['Custom.RollupCompleted']) {
					completed = uwi.fields['Custom.RollupCompleted']
				}

				RollupInfo rollupInfo = new RollupInfo([estimate:estimate, remaining:remaining, completed:completed])
				outInfo.add(rollupInfo)
			}
		}
		return outInfo
	}
	

}

@Canonical
class RollupInfo {
	int estimate = 0;
	int remaining = 0;
	int completed = 0;

}
