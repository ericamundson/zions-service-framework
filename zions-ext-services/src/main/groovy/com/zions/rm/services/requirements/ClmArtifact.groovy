package com.zions.rm.services.requirements

class ClmArtifact {
	String format
	String tfsWorkitemType
	String fileHref
	def attributeMap
	def collectionArtifacts
	def links
	public ClmArtifact(String in_title, String in_format, String in_about) {
		attributeMap = [:]
		collectionArtifacts = []
		if (in_title != null) {
			setTitle(in_title) // Needs to be in attributeMap because it will map over to ADO attribute
		}
		this.setAbout(in_about)
		format = in_format
	}
	public ClmArtifact(String in_about) {
		attributeMap = [:]
		collectionArtifacts = []
		this.setAbout(in_about)
	}
	public void setAbout(String in_about) {
		attributeMap.'about' = in_about
	}
	public String getAbout() {
		return attributeMap.'about'
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
		"${this.getID()}-Requirement"
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
