package com.zions.pipeline.services.mixins
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

trait YamlTrait {
	Map<String, String> schemaCache = [:]
	
	ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
	
	JsonSchemaFactory factory = JsonSchemaFactory.builder(
		JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
	)
	.objectMapper(mapper)
	.build()

	def validateYaml(String yaml, String version, String type) {
		String schema = null
		if (schemaCache.containsKey(version)) {
			schema = schemaCache[version]
		} else {
			schema = loadSchema(version)
			if (schema) {
				schema = schema.replace('\t', '  ')
			}
			schemaCache[version] = schema
		}
		if (!schema) {
			return ["${type}: No schema defined to validate yaml!"]
		}
		Set invalidMessages = factory.getSchema(schema).validate(mapper.readTree(yaml))
		Set outmessages = []
		if (!invalidMessages.empty) {
			for (String imessage in invalidMessages) {
				outmessages.add("${type}: ${imessage}")
				//log.error("${type}: ${imessage}")
			}
		}
		return outmessages
	}
	
	String loadSchema(String version) {
		InputStream is = this.getClass().getResourceAsStream("/${version}.json")
		
		if (!is) {
			URL url = this.getClass().getResource("/${version}.json")
			if (url) {
				File f = new File(url.toURI())
				return f.text
			}
			return null
		}
		return is.text
	}
}
