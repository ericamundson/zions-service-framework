package com.zions.rm.services.requirements

class ClmRequirementsModule  extends ClmArtifact {
	def orderedArtifacts
	public ClmRequirementsModule(in_title, in_format, in_about, String in_type, def in_attributes, def in_artifacts) {
		super(in_title, in_format, in_about)
		this.setBaseArtifactURI(in_about) // Base URI for module is same as about href
		artifactType = in_type
		attributeMap << in_attributes // Add to base Artifact attributes
		orderedArtifacts = in_artifacts
	}	
	
	def checkForDuplicates() {
		def ubound = this.orderedArtifacts.size() - 1
		0.upto(ubound, {
			def id = this.orderedArtifacts[it].getID()
			for (int i = 0; i < it-1; i++) {
				if (this.orderedArtifacts[i].getIsDeleted() == false && this.orderedArtifacts[i].getID() == id) {
					// Set the TFS work item type of this duplicate item from the 1st instance, since we won't be processing changes for this instance
					this.orderedArtifacts[it].setTfsWorkitemType(this.orderedArtifacts[i].getTfsWorkitemType())
					this.orderedArtifacts[it].setIsDuplicate(true)
				}
			}
		})
	}
}
