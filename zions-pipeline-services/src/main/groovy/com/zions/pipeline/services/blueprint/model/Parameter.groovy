package com.zions.pipeline.services.blueprint.model
import groovy.transform.Canonical

@Canonical
class Parameter {
	String name
	String type
	String prompt
	String description
	String adefault
	List<String> options
	String label
	String promptIf
	String validate
}
