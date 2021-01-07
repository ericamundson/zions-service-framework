package com.zions.pipeline.services.blueprint.model
import groovy.transform.Canonical

@Canonical
class Folder {
	String name
	String title
	String parentName
	List<Folder> folders = []
	List<Blueprint> blueprints = []
}
