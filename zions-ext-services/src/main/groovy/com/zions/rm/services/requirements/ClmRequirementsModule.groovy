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
}
