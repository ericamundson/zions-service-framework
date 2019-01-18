package com.zions.rm.services.requirements

class ClmModuleElement {
	boolean isHeading
	boolean isDuplicate
	String format
	int depth
	String baseArtifactURI
	String about
	String artifactType
	String tfsWorkitemType
	String fileHref
	boolean isDeleted
	def attributeMap
	def links
	
	public ClmModuleElement(String in_title, String in_baseURI, int in_depth, String in_format, String in_isHeading, String in_about) {
		// TODO Auto-generated constructor stub
		attributeMap = [:]
		attributeMap.put("title", in_title)
		baseArtifactURI = in_baseURI
		depth = in_depth
		format = in_format
		isHeading = (in_isHeading == "true")
		about = in_about
		isDeleted = false
		isDuplicate = false  // Assume false until checkDuplicate has been run

	}
	
	public boolean isHeading() {
		return isHeading;
	}
	public boolean isToIncorporateTitle() {
		return (artifactType == 'Supporting Material' ||
				artifactType == 'Scope' ||
				artifactType == 'Out of Scope' ||
				artifactType == 'Assumption' )
	}
	public String getTitle() {
		return attributeMap.'title'
	}
	public String getID() {
		return attributeMap.'Identifier'
	}
	public String setID(in_id) {
		attributeMap.'Identifier' = in_id
	}
	public void setTitle(String in_title) {
		attributeMap.'title' = in_title
	}
	public void setDescription(String in_desc) {
		attributeMap.'Primary Text' = in_desc
	}
}
