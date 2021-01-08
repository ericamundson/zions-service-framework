package com.zions.pipeline.services.blueprint.model

import groovy.transform.Canonical

@Canonical
class Blueprint {
	String title
	String description
	String path
	String name
	String repoUrl
	String[] outDir
	String outRepoName
	def permissions
	String selectedProjectParm
	List<Parameter> parameters = []
}
