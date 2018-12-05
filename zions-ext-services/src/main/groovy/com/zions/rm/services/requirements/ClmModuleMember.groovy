package com.zions.rm.services.requirements

class ClmModuleMember {
	boolean isHeading
	String format
	int depth
	String baseURI
	String title
	int identifier
	String about
	public ClmModuleMember(long in_id, String in_title, String in_baseURI, int in_depth, String in_format, String in_isHeading, String in_about) {
		// TODO Auto-generated constructor stub
		identifier = in_id
		title = in_title
		baseURI = in_baseURI
		depth = in_depth
		format = in_format
		isHeading = (in_isHeading == "true")
		about = in_about
	}

}
