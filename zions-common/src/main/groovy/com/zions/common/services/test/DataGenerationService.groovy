package com.zions.common.services.test

import groovy.json.JsonSlurper
import groovy.yaml.YamlSlurper
import groovy.xml.XmlSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Add for unit testing.  Enabling templating test data files and adding random data.
 * 
 * @author z091182
 *
 */
@Component
class DataGenerationService {
	
	
	@Autowired
	public Map<String, Generator> generatorMap
	
	public DataGenerationService() {
		
	}
	
	/**
	 * Generates a Map from input json file with random data of template.
	 * 
	 * @param template
	 * @return JsonSlurper result
	 */
	def generate(File template) {
		String name = template.name
		String outData = parse(template)
		if (name.endsWith('.json')) {
			return new JsonSlurper().parseText(outData)
		} else if (name.endsWith('.yaml')) {
				return new YamlSlurper().parseText(outData)
		}
		return new XmlSlurper().parseText(outData)

	}
	
	def generate(URL url) {
		File template = new File(url.file)
		String name = template.name
		String outData = parse(template)
		if (name.endsWith('.json')) {
			return new JsonSlurper().parseText(outData)
		} else if (name.endsWith('.yaml')) {
				return new YamlSlurper().parseText(outData)
		}
		return new XmlSlurper(false,false).parseText(outData)
	}

	/**
	 * Generates from a classpath resource template.
	 * 
	 * @param resource - class path resource name
	 * @return Parser result, either Xml or Json
	 */
	def generate(String resource, boolean raw = false) {
		URL url = this.getClass().getResource(resource)
		File template = new File(url.file)
		String outData = parse(template)
		if (!raw) {
			if (resource.endsWith('.json')) {
				return new JsonSlurper().parseText(outData)
			} else if (resource.endsWith('.yaml')) {
				return new YamlSlurper().parseText(outData)
			}
			return new XmlSlurper().parseText(outData)
		}
		return outData
	}

	private def parse(File template) {
		String data = template.text
		String outData = ""
		int startBracket = data.indexOf('{{')
		while (startBracket > -1) {
			int endBracket = data.indexOf('}}')
			outData = outData + data.substring(0, startBracket)
			String name = data.substring(startBracket+2, endBracket)
			def generator = generatorMap[name]
			if (generator != null) {
				def value = generator.gen()
				outData = "${outData}${value}"
			
			} else {
				outData = "${outData}{{${name}}}"
			}
			data = data.substring(endBracket+2)
			startBracket = data.indexOf('{{')
		}
		outData = "${outData}${data}"
		return outData
	}

}


