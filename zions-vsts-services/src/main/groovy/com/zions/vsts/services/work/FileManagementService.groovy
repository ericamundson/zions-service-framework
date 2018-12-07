package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Handles sending files to ADO to be used as attachments.
 * 
 * <p><b>Design:</b></p>
 * <img src="FileManagementService.png"/>
 * 
 * @author z091182
 *
 * @startuml
 * class FileManagementService [[java:com.zions.vsts.services.work.FileManagementService]] {
 * 	-IGenericRestClient genericRestClient
 * 	-WorkManagementService workManagementService
 * 	+FileManagementService()
 * 	-def encodeFile(Object data)
 * 	~def ensureAttachments()
 * 	-boolean linkExists()
 * 	-def uploadAttachment()
 * }
 * annotation Component
 * annotation Autowired
 * 
 * FileManagementService ..> Component
 * FileManagementService ..> Autowired
 * FileManagementService --> com.zions.common.services.rest.IGenericeRestClient: @Autowired genericRestClient
 * FileManagementService --> WorkManagementService: @Autowired workManagementService
 * @enduml
 */
@Component
@Slf4j
class FileManagementService {
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private WorkManagementService workManagementService;

	public FileManagementService() {
		
	}
	
	private def encodeFile( Object data ) throws UnsupportedEncodingException {
	    if ( data instanceof File ) {
	        def entity = new org.apache.http.entity.FileEntity( (File) data, "application/octet-stream" );
	        entity.setContentType( "application/octet-stream" );
	        return entity
	    } else {
	        throw new IllegalArgumentException( 
	            "Don't know how to encode ${data.class.name} as a zip file" );
	    }
	}
	
	/**
	 * Adds files to ADO, Then returns DTO's to send to associate attachments to work item.
	 * 
	 * @param collection - collection name 
	 * @param project - project name
	 * @param id - RTC work item ID
	 * @param files - list of File objects
	 * @return Work item update data
	 */
	def ensureAttachments(collection, project, id, files) {
		def cacheWI = workManagementService.getCacheWI(id)
		if (cacheWI != null) {
			def cid = cacheWI.id
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			files.each { fileItem ->
				File file = fileItem.file
				
				if (!linkExists(cacheWI, file)) {
					def area = cacheWI.fields.'System.AreaPath'
					def uploadData = uploadAttachment(collection, project, area, file)
					if (uploadData != null) {
						String comment = "${fileItem.comment}"
						def change = [op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: uploadData.url, attributes:[comment: comment]]]
						wiData.body.add(change)
					}
				}
			}
			if (wiData.body.size() == 1) {
				return null
			}
			return wiData
		}
		return null
	}
	
	private boolean linkExists(cacheWI, file) {
		String fileName = "${file.name}"
		def link = cacheWI.relations.find { rel ->
			def name = ""
			if (rel.attributes != null && rel.attributes.name != null) {
				name = "${rel.attributes.name}"
			}
			"${rel.rel}" == 'AttachedFile' && "${name}" == "${fileName}"
		}
		return link != null
	}

	private def uploadAttachment(collection, project, area, File file) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def currentEncoder = genericRestClient.delegate.encoder.'application/octet-stream'
		genericRestClient.delegate.encoder.'application/json' = this.&encodeFile
		def efilename = URLEncoder.encode(file.name, 'utf-8').replace('+', '%20')
		def earea = URLEncoder.encode(area, 'utf-8').replace('+', '%20')
		def result = genericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			requestContentType: ContentType.BINARY,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/attachments",
			body: file,
			query: ['api-version': '5.0-preview.3', uploadType: 'Simple', areaPath: earea, fileName: efilename]
			
			)
		genericRestClient.delegate.encoder.'application/octet-stream' = currentEncoder
		return result
	}
}
