package com.zions.pipeline.services.blueprint.model

import groovy.transform.Canonical

@Canonical
class Blueprint {
	String description
	String path
	String name
	String repoUrl
	String[] outDir
	String outRepoName
	def permissions 
	List<Parameter> parameters = []
}
