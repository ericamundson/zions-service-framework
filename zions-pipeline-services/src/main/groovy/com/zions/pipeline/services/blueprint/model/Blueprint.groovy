package com.zions.pipeline.services.blueprint.model

import groovy.transform.Canonical

@Canonical
class Blueprint {
	String path
	String name
	String repoUrl
	String[] outDir
	List<Parameter> parameters = []
}
