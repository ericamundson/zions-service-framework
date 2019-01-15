package com.zions.rm.services.requirements

class ClmRequirementsModule {
	// Define attributes for title, attributes and members
	String artifactType
	def attributeMap
	def orderedArtifacts
	public ClmRequirementsModule(String in_type, def in_attributes, def in_artifacts) {
		// TODO Auto-generated constructor stub
		artifactType = in_type
		attributeMap = in_attributes
		orderedArtifacts = in_artifacts
	}	
	
	def getTitle() {
		attributeMap.'title'
	}
	
	def checkForDuplicate(def index) {
		def id = this.orderedArtifacts[index].getID()
		for (int i = 0; i < index-1; i++) {
			if (this.orderedArtifacts[i].getIsDeleted() == false && this.orderedArtifacts[i].getID() == id) {
				// Set the TFS work item type of this duplicate item from the 1st instance, since we won't be processing changes for this instance
				this.orderedArtifacts[index].setTfsWorkitemType(this.orderedArtifacts[i].getTfsWorkitemType())
				this.orderedArtifacts[index].setIsDuplicate(true)
				return true
			}
		}
		return false
	}
}
