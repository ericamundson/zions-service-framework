package com.zions.rm.services.requirements

class ClmModuleElement extends ClmArtifact {
	boolean isHeading
	boolean isDuplicate
	int depth
	String baseArtifactURI
	String fileHref
	boolean isDeleted
	def collectionArtifacts
	
	public ClmModuleElement(String in_title, String in_baseURI, int in_depth, String in_format, String in_isHeading, String in_about) {
		// TODO Auto-generated constructor stub
		super(in_title, in_format, in_about)
		baseArtifactURI = in_baseURI
		depth = in_depth

		isHeading = (in_isHeading == "true")
		isDeleted = false
		isDuplicate = false  // Assume false until checkDuplicate has been run
		collectionArtifacts = []
	}
	
	public boolean isHeading() {
		return isHeading;
	}
	public boolean isToIncorporateTitle() {
		return (artifactType == 'Supporting Material' ||
				artifactType == 'Scope' ||
				artifactType == 'Out of Scope' ||
				artifactType == 'Assumption' ||
				artifactType == 'Issue' )
	}
	public incrementDepth(def incr) {
		this.depth = this.depth + incr
	}
}
