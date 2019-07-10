package com.zions.rm.services.requirements

class ModuleLink {
	String type
	String uri
	public ModuleLink(in_type,in_uri) {
		type = in_type
		uri = in_uri
	}
}
class ClmRequirementsModule  extends ClmArtifact {
	def orderedArtifacts
	public ClmRequirementsModule(in_title, in_format, in_about, String in_type, def in_attributes, def in_artifacts, def in_links) {
		super(in_title, in_format, in_about)
		this.setBaseArtifactURI(in_about) // Base URI for module is same as about href
		this.artifactType = in_type
		this.attributeMap << in_attributes // Add to base Artifact attributes
		this.orderedArtifacts = in_artifacts
		this.links = in_links
	}	

	def getLinkForType(String in_type) {
		// Return the first link for given type
		String uri = null
		if (links.size() > 0) {
			links.each { link ->
				if (link.type == in_type) {
					uri = link.uri
					return
				}	
			}
		}
		return uri
	}
	def appendModule(ClmRequirementsModule append_module) {
		// first add the module artifact as the first module element 
		ClmModuleElement moduleElement = new ClmModuleElement(1, append_module.getFormat(), append_module.attributeMap)
		this.orderedArtifacts.add(moduleElement)
		
		// now add all of the module's orderedArtifacts
		append_module.orderedArtifacts.each { artifact->
			this.orderedArtifacts.add(artifact)
		}
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
