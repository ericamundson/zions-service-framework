package com.zions.rm.services.requirements

class ClmModuleElement extends ClmArtifact {
	boolean isHeading
	boolean isDuplicate
	int depth
	String baseArtifactURI
	boolean isDeleted

	
	public ClmModuleElement(String in_title, String in_baseURI, int in_depth, String in_format, String in_isHeading, String in_about) {
		super(in_title, in_format, in_about)
		baseArtifactURI = in_baseURI
		depth = in_depth

		isHeading = (in_isHeading == "true")
		isDeleted = false
		isDuplicate = false  // Assume false until checkDuplicate has been run

	}

	public incrementDepth(def incr) {
		this.depth = this.depth + incr
	}
}
