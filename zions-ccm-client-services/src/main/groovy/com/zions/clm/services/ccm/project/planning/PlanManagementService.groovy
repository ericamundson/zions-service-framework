package com.zions.clm.services.ccm.project.planning

import java.util.TimeZone

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.ibm.team.process.client.IProcessItemService
import com.ibm.team.process.common.IDevelopmentLine
import com.ibm.team.process.common.IDevelopmentLineHandle
import com.ibm.team.process.common.IIteration
import com.ibm.team.process.common.IIterationHandle
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.process.common.ITeamArea
import com.ibm.team.process.common.ITeamAreaHandle
import com.ibm.team.process.common.ITeamData
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.client.internal.ItemManager
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.model.ICategory
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.helper.DevelopmentLineHelper
import com.zions.clm.services.ccm.utils.ProcessAreaUtil

/**
 * Provides behavior to access planning data from IBM RTC.
 * o Get project categories so VSTS can duplicate to team areas.
 * 
 * @author z091182
 *
 */
@Component
class PlanManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	public PlanManagementService() {
		
	}
	
	def getIterations(String project) {
		
	}
	/**
	 * Get the project area.
	 * @param projectArea
	 * @return
	 */
	private IProjectArea findProjectArea(def projectArea) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IProcessItemService  service = (IProcessItemService) teamRepository.getClientLibrary(IProcessItemService.class);
		def pAreas = service.findAllProjectAreas(null, null)
		IProjectArea retVal = null
		pAreas.each { IProjectArea pArea ->
			if (pArea.getName() == "${projectArea}") {
				retVal = pArea
			}
		}
		return retVal
	}

	def getIterations(String tfsRootArea, String project) {
		IProjectArea projectArea = findProjectArea(project)
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		DevelopmentLineHelper dhelper = new DevelopmentLineHelper(teamRepository, rtcRepositoryClient.getMonitor())
		IDevelopmentLineHandle[] devLines = projectArea.getDevelopmentLines();
		def iterationData = ['iterations': [], 'teams': [:]]
		devLines.each { dh ->
			IDevelopmentLine dl = dhelper.resolveDevelopmentLine(dh)
			
			if (!dl.isArchived()) {
				String label = dl.getLabel()
				
				Date sd = dl.getStartDate()
				Long sds = null 
				if (sd != null) {
					sds = sd.time
				}
				Date ed = dl.getEndDate()
				Long eds = null 
				if (ed != null) {
					Calendar cal = Calendar.instance(ed)
					cal = cal.add(Calendar.DATE, -1)
					eds = cal.time
				}
				def iteration = [name: "${dl.getLabel()}", 'id': "${dl.getLabel()}", startDate: sds, endDate:eds, children: []]
				IIterationHandle[] ith = dl.getIterations()
				iteration = processChildren(dhelper, ith, iteration)
				iterationData.iterations.add(iteration)
			}
		}
		return iterationData
	}
	
	def processChildren(DevelopmentLineHelper dhelper, IIterationHandle[] iths, iteration) {
		iths.each { IIterationHandle ith ->
			IIteration it = dhelper.resolveIteration(ith)
			if (!it.isArchived()) {
				String fullPath = "${iteration.id}/${it.getLabel()}"
				Date sd = it.getStartDate()
				Long sds = null 
				if (sd != null) {
					sds = sd.time
				}
				Date ed = it.getEndDate()
				Long eds = null 
				if (ed != null) {
					eds = ed.time
				}
				def citeration = [name: "${it.getLabel()}", 'id': "${fullPath}", startDate: sds, endDate: eds, children: []]
				IIterationHandle[] its = it.getChildren()
				
				citeration = processChildren(dhelper,its, citeration)
				iteration.children.add(citeration)
			}
		}
		return iteration
	}
	
	/**
	 * Get categories from project area.  Build data structure to pass to VSTS for team work areas.
	 * 
	 * @param tfsRootArea
	 * @param project
	 * @return
	 */
	def getCategories(String tfsRootArea, String project) {
		IProjectArea projectArea = findProjectArea(project)
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		List categories = workItemClient.findAllCategories(projectArea, ICategory.FULL_PROFILE, null);
		def categoryData = ['areas': [], 'teams': [:]]
		for (ICategory iCategory : categories) {
			if (!iCategory.archived) {
				def id = convertId(iCategory.categoryId.getSubtreePattern(), tfsRootArea)
				if ("${id}" == "") {
					id = 'root'
				}
				def cat = ['name': iCategory.name, 'id': id, 'children': []]
				def parent = getParent(categoryData.areas, iCategory, tfsRootArea)
				if (parent != null) {
					parent.children.add(cat)
				}
				List<ITeamAreaHandle> tahs = iCategory.getAssociatedTeamAreas()
				def teams = []
				for (ITeamAreaHandle h: tahs) {
				
					ITeamArea teamArea = rtcRepositoryClient.getRepo().itemManager().fetchCompleteItem(h, ItemManager.DEFAULT,  null)
					if (categoryData.teams["${teamArea.name}"] == null) {
						categoryData.teams["${teamArea.name}"] = []
						categoryData.teams["${teamArea.name}"].add(cat)
					} else {
						categoryData.teams["${teamArea.name}"].add(cat)
					}
				}
				//cat['teams'] = teams
				if (parent == null) {
					categoryData.areas.add( cat )
				}
			}
			//System.out.println("\tID: " + iCategory.getCategoryId() + "\tName: " + iCategory.getName());
		}
		categoryData.areas[0].name = tfsRootArea
		return categoryData
	}
	def convertId(id, tfsRootArea) {
		def idOut = "${id}"
		idOut = idOut.replace('/Unassigned', tfsRootArea)
		idOut = idOut.replace('/%', '')
		
	}
	def getParent(def areas, ICategory cat,def tfsRootArea) {
		def retVal = null
		def catId = null
		if (cat.parentId2 != null) {
			catId = convertId(cat.parentId2.getSubtreePattern(), tfsRootArea)
		}
		areas.each { icat -> 
			if ("${icat.id}" == "${catId}") {
				retVal = icat
			} else {
				retVal = getParent(icat.children)
			}
		}
		return retVal
	}

}
