package com.zions.rm.services.requirements

class ClmModuleElement {
	boolean isHeading
	String format
	int depth
	String baseArtifactURI
	String about
	String artifactType
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

	}
	
	public getArtifactType() {
		return artifactType
	}
	public boolean isHeading() {
		return isHeading;
	}
	public boolean isSupportingMaterial() {
		return (artifactType == "Supporting Material")
	}
	public void setTitle(String in_title) {
		attributeMap.'title' = in_title
	}
	public String getTitle() {
		return attributeMap.'title'
	}
	public String getID() {
		return attributeMap.'Identifier'
	}
}
