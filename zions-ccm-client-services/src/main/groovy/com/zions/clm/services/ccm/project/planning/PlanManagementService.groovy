package com.zions.clm.services.ccm.project.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.ibm.team.process.client.IProcessItemService
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.process.common.ITeamArea
import com.ibm.team.process.common.ITeamAreaHandle
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.client.internal.ItemManager
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.model.ICategory
import com.zions.clm.services.ccm.client.RtcRepositoryClient

@Component
class PlanManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	public PlanManagementService() {
		
	}
	
	def getIterations(String project) {
		
	}
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

	def getCategories(String tfsRootArea, String project) {
		IProjectArea projectArea = findProjectArea(project)
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		List categories = workItemClient.findAllCategories(projectArea, ICategory.FULL_PROFILE, null);
		def categoryData = ['areas': [], 'teams': [:]]
		for (ICategory iCategory : categories) {
			if (!iCategory.archived) {
				def id = convertId(iCategory.categoryId.getSubtreePattern(), tfsRootArea)
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
