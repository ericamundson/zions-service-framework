package com.zions.vsts.services.extdata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.extension.IExtensionData
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * <lu>
 * <li>
 * To create post example:  https://extmgmt.dev.azure.com/eto-dev/_apis/ExtensionManagement/InstalledExtensions/Zions/vsts-extensions-custom-attributes-control/Data/Scopes/Default/Current/Collections/eto-dev/Documents
 * post body is doc json
 * </li>
 * <li>
 * To update post example:  https://extmgmt.dev.azure.com/eto-dev/_apis/ExtensionManagement/InstalledExtensions/Zions/vsts-extensions-custom-attributes-control/Data/Scopes/Default/Current/Collections/eto-dev/Documents/{id}
 * post body is doc json
 * </li>
 * <li>
 * To GET example:  https://extmgmt.dev.azure.com/eto-dev/_apis/ExtensionManagement/InstalledExtensions/Zions/vsts-extensions-custom-attributes-control/Data/Scopes/Default/Current/Collections/eto-dev/Documents/{id}
 * get body is doc json
 * </li>
 * </lu><p>
 * Key example:  let id: string = wiType + '_' + this.fieldName + '_' + project
 * @author z091182
 *  
 */
@Component
@Slf4j
class ExtensionDataManagementService implements IExtensionData {
	@Value('${ext.url:}')
	String extUrl
	
	@Value('${ext.publisher:}')
	String extPublisher
	
	@Value('${ext.name:}')
	String extName
	
	@Value('${ext.collection:eto-dev}')
	String tfsCollection
	
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	def getExtensionData(String key) {
		def ekey = URLEncoder.encode(key, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${extUrl}/_apis/ExtensionManagement/InstalledExtensions/${extPublisher}/${extName}/Data/Scopes/Default/Current/Collections/${tfsCollection}/Documents/${ekey}",
			headers: [Accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true']
			//query: query,
			)
		return result
	}
	
	def ensureExtensionData(def data) {
		String id = data.id
		
		def doc = getExtensionData(id)
		if (!doc) {
			doc = createExtensionData(data)
		} else {
			data.__etag = doc.__etag
			doc = updateExtensionData(data)
		}
		return doc
	}
	
	private def createExtensionData(def data) {
		String body = new JsonBuilder(data).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${extUrl}/_apis/ExtensionManagement/InstalledExtensions/${extPublisher}/${extName}/Data/Scopes/Default/Current/Collections/${tfsCollection}/Documents",
			body: body,
			headers: [Accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true']
			)
		return result

	}
	
	private def updateExtensionData(def data) {
		String id = data.id
		String body = new JsonBuilder(data).toString()
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${extUrl}/_apis/ExtensionManagement/InstalledExtensions/${extPublisher}/${extName}/Data/Scopes/Default/Current/Collections/${tfsCollection}/Documents",
			body: body,
			headers: [Accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true']
			)
		return result

	}
}
