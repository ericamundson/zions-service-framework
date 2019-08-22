package com.zions.rm.services.requirements

class ClmArtifact {
	String format
	String tfsWorkitemType
	String fileHref
	String typeSeqNo
	def attributeMap = [:]
	def collectionArtifacts = []
	def links = []
	def changes = [:]
	def adoFileInfo = []
	def cacheWI // cached work item from ADO
	boolean isNew = false
	public ClmArtifact(String in_title, String in_format, String in_about) {
		setTitle(in_title)
		this.setAbout(in_about)
		format = in_format
	}
	public ClmArtifact(String in_format, def in_attributeMap) {
		attributeMap = in_attributeMap
		format = in_format
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
		"${this.getID()}"  // Return Rational ID
	}
	public String setID(in_id) {
		attributeMap.'Identifier' = in_id
	}
	public void setDescription(String in_desc) {
		if (in_desc == null) in_desc = ''
		// If this is a Heading, use Primary Text for the Title (this is a weird thing DNG modules do)
		if (this.getArtifactType() == 'Heading' && in_desc != '') {
			this.setTitle(stripTags(in_desc))
			attributeMap.'Primary Text' = '' // For appearance in SmartDocs, don't want to duplicate title
		}
		else {
			attributeMap.'Primary Text' = in_desc			
		}

	}
	public String getDescription() {
		return attributeMap.'Primary Text'
	}
	public void setLinks(def linklist) {
		links = linklist
	}
	public String getWhereUsed() {
		return attributeMap.'Where Used'
	}
	public void setBaseArtifactURI(String in_uri) {
		attributeMap.'Base Artifact URI' = in_uri
	}
	public void setBaseArtifactURI(String in_base_uri, String in_uid) {
		attributeMap.'Base Artifact URI' = "${in_base_uri}/rm/resources/${in_uid}"
	}
	public String getBaseArtifactURI() {
		return attributeMap.'Base Artifact URI'
	}
	public boolean hasEmbeddedImage() {
		if (this.getDescription() == null) {
			return false
		}
		else {
			return (this.getDescription().indexOf('<img ') > -1)
		}
	}
	public String stripTags(String input) {
		if (input) {
			return input.replaceAll("&lt;",'<').replaceAll("&gt;",'>').replaceAll("&#xa0;", ' ').replaceAll("&#xc2;", ' ').replaceAll("&amp;", '&').replaceAll("\\<.*?>","")
		}
		else {
			return input
		}
			
	}
	public def getAttribute(String attrName) {
		def val = attributeMap."$attrName"
		if (val) {
			return val.replaceAll('\\r\\n|\\r|\\n', '<br>')
		}
		else {
			return ''
		}
	}
	public void setTypeSeqNo(def seqNo) {
		typeSeqNo = String.valueOf(seqNo)
	}

}
