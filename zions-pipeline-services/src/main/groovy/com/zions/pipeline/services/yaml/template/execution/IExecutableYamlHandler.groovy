package com.zions.pipeline.services.yaml.template.execution

interface IExecutableYamlHandler {
	def handleYaml(def yaml, File repo, def locations, String branch);
}
