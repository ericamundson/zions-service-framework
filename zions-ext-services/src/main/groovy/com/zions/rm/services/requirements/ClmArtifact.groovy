package com.zions.rm.services.requirements

class ClmArtifact {
	String about
	String artifactType
	String format
	String tfsWorkitemType
	def attributeMap
	def links
	public ClmArtifact(String in_title, String in_format, String in_about) {
		attributeMap = [:]
		attributeMap.put("title", in_title)
		about = in_about
		format = in_format
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
	public String setID(in_id) {
		attributeMap.'Identifier' = in_id
	}
	public void setDescription(String in_desc) {
		attributeMap.'Primary Text' = in_desc
	}
}
