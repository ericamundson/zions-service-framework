package com.zions.rm.services.requirements

class ClmModuleElement extends ClmArtifact {
	boolean isHeading
	boolean isDuplicate
	int depth
	boolean isDeleted

	
	public ClmModuleElement(String in_title, int in_depth, String in_format, String in_isHeading, String in_about) {
		super(in_title, in_format, in_about)
		depth = in_depth

		isHeading = (in_isHeading == "true")
		isDeleted = false
		isDuplicate = false  // Assume false until checkDuplicate has been run
	}
	
	public ClmModuleElement(int in_depth, def in_format, def in_attributeMap) {
		super(in_format, in_attributeMap)
		depth = in_depth

		isHeading = false
		isDeleted = false
		isDuplicate = false  // Assume false until checkDuplicate has been run
	}

	public incrementDepth(def incr) {
		this.depth = this.depth + incr
	}
}
