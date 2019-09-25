package com.zions.rm.services.requirements

class ClmRequirementsModule  extends ClmArtifact {
	def orderedArtifacts
	String appendedDocumentType
	String satisfiesLink
	public ClmRequirementsModule(String in_title, String in_format, String in_about, String in_type, String in_satisfiesLink, def in_attributes, def in_artifacts) {
		super(in_title, in_format, in_about)
		this.setBaseArtifactURI(in_about) // Base URI for module is same as about href
		this.artifactType = in_type
		this.attributeMap << in_attributes // Add to base Artifact attributes
		this.orderedArtifacts = in_artifacts
		this.satisfiesLink = in_satisfiesLink
	}	
	def appendModule(ClmRequirementsModule append_module) {
		// NOTE: We increment the depth on all appended module artifacts so as to preserve outline numbering
		// first add the module artifact as the first module element 
		def startDepth = 2
		ClmModuleElement moduleElement = new ClmModuleElement(startDepth, append_module.getFormat(), append_module.attributeMap)
		this.orderedArtifacts.add(moduleElement)
		
		// now add all of the module's orderedArtifacts
		append_module.orderedArtifacts.each { artifact->
			artifact.incrementDepth(2)
			this.orderedArtifacts.add(artifact)
		}
		
		// Set appendedDocumentType
		this.appendedDocumentType = append_module.getArtifactType()
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
	def setDocumentType() {
		
	}
	String getTargetFolder() {
		// Get module artifact type 
		String artifactType = this.getArtifactType()
		if (artifactType == 'Functional Spec') {
			return '/R3/Functional Specs'
		}
		else if (artifactType == 'Interface Spec') {
			return '/R3/ISZ'
		}
		else if (artifactType == 'UI Spec') {
			return '/R3/Screen Alignment'
		}
		else if (artifactType == 'Reporting RRZ' || this.appendedDocumentType == 'Reporting RRZ') {
			return '/R3/Reporting'
		}
		else if (artifactType == 'Statements and Notices RRZ Spec' || this.appendedDocumentType == 'Statements and Notices RRZ Spec') {
			return '/R3/Statements and Notices'
		}
		else {
			return '/' // Put it in root
		}
	}
}
