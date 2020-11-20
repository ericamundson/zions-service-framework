package com.zions.vsts.services.mr

import com.zions.vsts.services.mr.SmartDocManagementService.WorkItemDetails

class SmartDoc {
	String name
	String folder
	String template
	List workItems = []
	String wiDetails
	String rootDocId
	public SmartDoc(String name, String folder, String template, def smdFile) {
		this.name = name
		this.template = template
		this.folder = folder
		parseSmdFile(smdFile)
	}
	
	public String getWorkItemDetails(String action) {
		// For an update, the work item details in MR API does not include the root document
		if (action == 'Update')
			return wiDetails
		else { // Create
			if (wiDetails)
				return """[{"id":"${rootDocId}","linkType":"","links":${wiDetails}}]"""
			else
				return """[{"id":"${rootDocId}","linkType":""}]"""
		}
	}

	private parseSmdFile(def smdFile) {
		rootDocId = "${smdFile.smartdocsmetainfo.id}"
		workItems.add(rootDocId)
		if ("${smdFile.smartdocsmetainfo.children}" != 'null') {
			wiDetails = getWorkitemDetails(smdFile.smartdocsmetainfo.children)
		}
	}
	private String getWorkitemDetails(def children) {
		boolean isFirst = true
		String jsonString = '['
		children.each { child ->
			if (!isFirst) {
					jsonString = jsonString + ',' + '\n'
			}
			if (child.children) {
				def wiChildDetails = getWorkitemDetails(child.children)
				jsonString = jsonString + """{"id":"${child.id}","linkType":"","links":${wiChildDetails}}"""
			}
			else {
				jsonString = jsonString + """{"id":"${child.id}","linkType":""}"""
			}
			workItems.add("${child.id}")
			isFirst = false
		}
		

		return jsonString + ']'
	}

}
