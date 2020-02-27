package com.zions.ext.services.cli.action.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlUtil

/**
 * Provides ability to test and save results of low level rest calls. 
 * 
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>restClient - The action's Spring bean name.</li>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - (optional) CLM password. It can be hidden in props file.</li>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO PAT</li>
 *  <li>request.file - json input file that relates to input map</li>
 *  <li>response.file - output file of rest result</li>
 *  <li>request.type - relates to http request type.  E.G. get, post, post, patch, delete, put</li>
 *  <li>client.name - genericRestClient for ADO, clmGenericRestClient for clm spring proile and will vary by Spring profile</li>
 *  <li>result.protocol - can be: json, xml</li>
 *  </ul>
 * </ul>
 * 
 * @author z091182
 */
@Component
class RestClient implements CliAction {
	
	@Autowired(required=false)
	Map<String, IGenericRestClient> clientMap

	@Override
	public Object execute(ApplicationArguments data) {
		String requestFileName = data.getOptionValues('request.file')[0]
		String responseFileName = data.getOptionValues('response.file')[0]
		String requestType = data.getOptionValues('request.type')[0]
		String resultProtocol = data.getOptionValues('result.protocol')[0]
		String clientName = data.getOptionValues('client.name')[0]
		File requestFile = new File(requestFileName)
		if (!requestFile.exists()) return null
		def requestMap = new JsonSlurper().parse(requestFile)
		IGenericRestClient client = clientMap[clientName]
		def result = null
		switch(requestType) {
			case 'get':
				result = client.get(requestMap)
				break
			case 'post':
				result = client.post(requestMap)
				break
			case 'patch':
				result = client.patch(requestMap)
				break
			case 'put':
				result = client.put(requestMap)
				break
			case 'delete':
				result = client.patch(requestMap)
				break
			default:
				result = client.get(requestMap)
				break

		}
		File ofile = new File(responseFileName)
		//if (!ofile.exists()) return null;
		if (resultProtocol == 'json' && result != null) {
			String json = new JsonBuilder(result).toPrettyString()
			def os = ofile.newDataOutputStream()
			os << json
			os.close()
		} else if (resultProtocol == 'xml' && result != null) {
			String xml = ''
			if (result instanceof ByteArrayInputStream) {
				byte[] bs = ((ByteArrayInputStream) result).bytes
				xml = new String(bs, 'utf-8')
			} else {
				xml = XmlUtil.serialize(result)
			}
			def os = ofile.newDataOutputStream()
			os << xml
			os.close();
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'tfs.url', 'tfs.user', 'tfs.token', 'request.file', 'response.file', 'request.type', 'client.name', 'result.protocol']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}
