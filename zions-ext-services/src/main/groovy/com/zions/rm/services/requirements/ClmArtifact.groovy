package com.zions.rm.services.requirements

class ClmArtifact {
	String about
	String format
	String tfsWorkitemType
	def attributeMap
	def links
	public ClmArtifact(String in_title, String in_format, String in_about) {
		attributeMap = [:]
		setTitle(in_title) // Needs to be in attributeMap because it will map over to ADO attribute
		about = in_about
		format = in_format
	}
	public void setTitle(String in_title) {
		attributeMap.'title' = in_title
	}
	public String getTitle() {
		return attributeMap.'title'
	}
	public void setArtifactType(String in_type) {
		attributeMap.'Artifact Type' = in_type
	}
	public String getArtifactType() {
		return attributeMap.'Artifact Type'
	}
	public String getID() {
		return attributeMap.'Identifier'
	}
	public String getCacheID() {
		"${this.getID()}-${this.getTfsWorkitemType()}"
	}
	public String setID(in_id) {
		attributeMap.'Identifier' = in_id
	}
	public void setDescription(String in_desc) {
		attributeMap.'Primary Text' = in_desc
	}
	public String getDescription() {
		return attributeMap.'Primary Text'
	}
}
