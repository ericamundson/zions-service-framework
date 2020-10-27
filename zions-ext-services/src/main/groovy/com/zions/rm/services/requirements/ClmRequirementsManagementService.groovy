package com.zions.rm.services.requirements

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import org.apache.commons.io.IOUtils



/**
 * Handles queries into DNG to navigate object structure of DNG.
 * 
 * Design<p/>
 * <img src="ClmRequirementsManagementService.svg"/>
 * 
 * @author z091182
 * 
 * @startuml
 * 
 * annotation Componentsdfdsasd
 * annotation Autowired
 * 
 * class ClmRequirementsManagementService {
 * ... Ideas on methods to implement ...
 * + def queryForModules(String project, String query )
 * + def nextPage(String url)
 * }
 * note left: @Component
 * 
 * ClmRequirementsManagementService .. Component: Is as Spring component
 * ClmRequirementsManagementService .. Autowired: Has autowired dependencies
 * ClmRequirementsManagementService o--> RmGenericRestClient: @Autowire rmGenericRestClient
 * 
 * @enduml
 *
 */

@Slf4j
@Component
class ClmRequirementsManagementService {
	@Autowired
	@Value('${clm.url}')
	String clmUrl
	
	@Autowired
	@Value('${clm.pageSize}')
	String clmPageSize
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired
	IGenericRestClient rmGenericRestClient
	
	@Autowired
	IGenericRestClient rmBGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	@Autowired(required=false)
	IDatabaseQueryService databaseQueryService
	
	@Value('${sql.resource.name:/sql/core.sql}') 
	String sqlResourceName
	

	
	ClmRequirementsManagementService() {
	}
	
	def queryForModules(String query) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/publish/collections?" + query;
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
		def moduleList = [:]
		// Get all module uris from collection
		if (result) {
			def modules =  result.'**'.findAll { p ->
			"${p.name()}" == 'Link'
			}
			modules.forEach { node ->
				Integer id
				String ref
				node.children().forEach { child ->
					if ("${child.name()}" == 'identifier') id = "$child".toInteger()
					if ("${child.name()}" == 'relation') ref = "$child"
				}
				if (id && ref) {
					moduleList.put(id,ref) 
				}
			}
		}
		else {
			log.info("Nothing returned from DNG query: $uri")
		}
		return moduleList.sort()
	}

	ClmRequirementsModule getModule(String moduleUri, boolean validationOnly) {
		boolean cacheLinks = !validationOnly
		boolean addEmbeddedCollections = false
		def satisfiesLinks = [] // For associated RRZ module
		String uri = moduleUri.replace('resources/','publish/modules?resourceURI=')
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'],
			contentType: ContentType.TEXT );
		// Replace special characters from Unicode translation
		String resultStr = result.str
		resultStr = fixSpecialCharacters(resultStr)
		
		// Use xmlSlurper to parse xml
		result = new XmlSlurper().parseText(resultStr)
		
		// Extract and instantiate the ClmRequirementsmodules
		def module = result.artifact[0]
		String moduleType = ""
		def moduleAttributeMap = [:]
		def orderedArtifacts = []
		
		// Extract module title, format, about
		String moduleTitle = module.title
		String moduleFormat = module.format
		String moduleAbout = module.about
		
		
		// Extract module attributes and members
		module.children().each { child ->
			String iName = child.name()
			if (iName == "collaboration" ) {
				// Set artifact type and attributes
				moduleType = parseCollaborationAttributes(child, moduleAttributeMap )
				if (!validationOnly) {
					addEmbeddedCollections = shouldAddCollectionsToModule(moduleType)
				}				
			}
			else if (iName == "traceability") {
				child.links.children().each { link ->
					String linkType
					String linkURI				
					link.children().each { linkattr ->

						if (linkattr.name() == 'title') {
							linkType = "${link.title}"
						}
						else if (linkattr.name() == 'relation') {
							linkURI = "${link.relation}"
						}
					}
					if (linkType == 'Satisfies') {
						satisfiesLinks.add(linkURI)
						return 
					}
				}
			}
			else if (iName == "moduleContext") {
				def kk = 0
				def prevType = null
				def seqNo = 0
				child.children().each { contextBinding ->
						String about = contextBinding.about
						String artifactTitle = contextBinding.title
						String format = contextBinding.format
						int depth = contextBinding.depth.toInteger()
						String isHeading = contextBinding.isHeading
						String baseUID = contextBinding.core
						
						ClmModuleElement artifact = new ClmModuleElement(artifactTitle, depth, format, isHeading, about)
						artifact.setBaseArtifactURI("${this.rmBGenericRestClient.clmUrl}","${baseUID}")
						
						orderedArtifacts.add(artifact)
						if (format == "Text") {
							// Get artifact details (attributes and links) from DNG
							getTextArtifact(artifact, true, cacheLinks)
							// If artifact has embedded collection, add collection members to the module
							if (shouldAddCollectionsToModule(moduleType) && artifact.collectionArtifacts != null && artifact.collectionArtifacts.size > 0) {
								artifact.setDescription(null)  // blank out Description content
								artifact.setArtifactType('Supporting Material')  // Collection container should now be just a Section in the module
								artifact.collectionArtifacts.each { ca ->
									orderedArtifacts.add(ca)
								}
							}
						}
						else {
							getNonTextArtifact(artifact, true, cacheLinks)
						}
						
						// Set typeSeqNo to migrate RPE generated sequence numbers
						if (artifact.getArtifactType()== prevType) {
							seqNo = seqNo + 1
							artifact.setTypeSeqNo(seqNo)
						}
						else {
							seqNo = 1
							artifact.setTypeSeqNo(seqNo)
						}
						prevType = artifact.getArtifactType()
				}
			}
		}
		
		
		// Instantiate and return the module
		ClmRequirementsModule clmModule = new ClmRequirementsModule(moduleTitle, moduleFormat, moduleAbout, moduleType, satisfiesLinks, moduleAttributeMap,orderedArtifacts)
		if (cacheLinks) parseLinksFromArtifactNode(module, clmModule)
		return clmModule

	}
	
	
	def queryDatawarehouseSource(Date ts) {
		String selectStatement = new SqlLoader().sqlQuery(sqlResourceName)
		String endDate = ts.format('MM/dd/yyyy')
		databaseQueryService.init()
		return databaseQueryService.query(selectStatement, [endDate: endDate])
	}
	
	def nextPageDb() {
		//log.debug("Retrieving nextPage from databasequeryservice")
		return databaseQueryService.nextPage()
	}
	
	def pageUrlDb() {
		return databaseQueryService.pageUrl()
	}
	
	def initialUrlDb(Date ts) {
		log.debug("Setting db select and returning initialUrl")
		if (!databaseQueryService.select) {
			queryDatawarehouseSource(ts)
		}
		return databaseQueryService.initialUrl()
	}

	//
	/**
	 * @param maxPage - For testing purposes
	 * @param delta - Set to true if doing an update so we do not overwrite the queryStart (used as the beginning of delta queries)
	 * @return
	 */
	def flushQueries(boolean delta = false, int maxPage = -1) {
		Date ts = new Date()
		Date queryEndDate;
		
		//if this is not a delta run, queryEndDate and the QueryStart value are identical as usual
		if (!delta) {
			log.info("Writing new QueryStart timestamp for RM")
			cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
			queryEndDate = ts;
		} else {
			//if this is a delta run, we want a new date for cache purposes (or it will reuse the original query pages)
			//but we want to not touch QueryStart as that is what our delta is based on
			//in theory this could shuffle to delta from the last delta, but it makes testing a pain for now
			def cp = cacheManagementService.getFromCache('query', 'QueryStart')
			if (cp) {
				cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'last', 'QueryDelta')
				queryEndDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			}
			log.info("Performing delta on update from original QueryStart date ${queryEndDate}")
		}
		int pageCount = 0
		def currentItems
		def iUrl

		new CacheInterceptor() {}.provideCaching(this, "${pageCount}", ts, DataWarehouseQueryData) {
			currentItems = this.queryDatawarehouseSource(queryEndDate)
		}
		while (true) {
			iUrl = this.pageUrlDb()
			pageCount++
			log.info("Saving page $pageCount from DataWarehouse to cache")
			if (maxPage != -1 && (maxPage) == pageCount) break; // For testing
			new CacheInterceptor() {}.provideCaching(this, "${pageCount}", ts, DataWarehouseQueryData) {
				currentItems = this.nextPageDb()
			}
			if(!currentItems) break;
		}
	}

	
	//Since the specified values can be autowired, I really feel like maybe
	//there should just be a getUri function and a queryForArtifacts(uri)
	//that returns the below data.  Some of the code seems overly duplicated.
	//not gonna mess with it though.
	def queryForArtifacts(String projectURI, String oslcNS, String oslcSelect, String oslcWhere) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/views?oslc.query=&projectURL=" + this.rmGenericRestClient.clmUrl + "/rm/process/project-areas/" + projectURI + 
					oslcNS + oslcSelect + oslcWhere.replace('zpath',this.rmGenericRestClient.clmUrl) + "&oslc.pageSize=${clmPageSize}";

		uri = uri.replace('<','%3C').replace('>', '%3E')
		log.debug("Live REST call: queryForArtifacts with uri: ${uri}")
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			log.debug("Live REST call queryForArtifacts finished, returning data")
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			log.debug("Live REST call queryForArtifacts returned no data for uri: ${uri}")
			return null;
		}
	}
	
	
	def queryForFolders(String folderURI) {
		String uri = this.rmGenericRestClient.clmUrl + "/rm/folders?oslc.where=public_rm:parent=" + folderURI
		uri = uri.replace('<','%3C').replace('>', '%3E')
		//uri = URLEncoder.encode(uri,'UTF-8')
		//println("Querying folder: " + uri)
		def result = rmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			log.debug ("failed to query folder: "+ folderURI)
			return null;
		}
	}
	
	public def nextPage(url) {
		log.debug("Live REST call: nextPage by url: ${url}")
		def result = rmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'application/rdf+xml', 'OSLC-Core-Version': '2.0'] );
		if (result != null) {
			log.debug("Live REST call for nextPage completed, returning data")
			String xml = IOUtils.toString(result, StandardCharsets.UTF_8)
			return new XmlSlurper().parseText(xml)
		}
		else {
			log.debug("Live REST call for nextPage returned no data")
			return null;
		}
	}
	
	//single giant query but should cache ok
	def queryForWhereUsed() {
//		String uri = this.rmBGenericRestClient.clmUrl + "/rs/query/11126/dataservice?report=11099&limit=-1&basicAuthenticationEnabled=true"
//		def results = rmBGenericRestClient.get(
//				uri: uri,
//				headers: [Accept: 'application/rdf+xml'] );
		String selectStatement = new SqlLoader().sqlQuery('/sql/whereUsed.sql')
		databaseQueryService.init()
		def results = databaseQueryService.query(selectStatement)
		log.info("Processing whereUsed info from data warehouse query...")
		cacheManagementService.deleteByType("whereUsedData")
		while (results) {
			processWhereUsedPage(results)
			this.pageUrlDb()
			results = this.nextPageDb()
		}
		return true;
		
//		else {
//			log.info("Null results from fetching whereUsed cache, something went wrong I wager")
//			return false
//		}
	}
	
	def processWhereUsedPage(def results) {
		//log.info("Fetched whereUsed info from JRS, now storing to cache")
		def prevID = null
		def whereUsedList = []
		results.each { p ->
			def id = "${p.reference_id}"
			//log.debug("${id}")
			if (prevID != null && id != prevID) { // Save whereUsed for this id
				cacheManagementService.saveToCache(whereUsedList, prevID, 'whereUsedData')
				whereUsedList.clear()
			}
			whereUsedList.add([name: "${p.module_name}", url: "${p.url2}"])
			//log.debug("name: ${p.module_name}, url: ${p.url2}")
			prevID = id
		}
		//log.debug("Stored ${whereUsedList.size()} whereUsed records")
		return true
	}
	
	//cool, hardcoded logic!
	boolean shouldAddCollectionsToModule(String moduleType) {
		return (moduleType == 'UI Spec' || moduleType == 'Functional Spec')
	}
	
	def getMemberEmail(String url) {
		String emailAddress = ""
		def result = rmGenericRestClient.get(
			uri: url.replace('jts', 'ccm/oslc'),
			headers: [Accept: 'application/xml'] );

		if (result != null) {
			result.children().each { node ->
			// Extract and instantiate the ClmRequirementsmodules
				if (node.name() == 'emailAddress') {
					emailAddress = node
					emailAddress = emailAddress.replace("mailto:","").replace("%40","@")
				}
			}
		}

		return emailAddress
	}
	
	def getTextArtifact(def in_artifact, boolean includeCollections, boolean cacheLinks) {
		//log.debug("Fetching text artifact")
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/text?resourceURI="),
				headers: [Accept: 'application/xml'],
			contentType: ContentType.TEXT );
		// Replace special characters from Unicode translation
		String resultStr = result.str
		resultStr = fixSpecialCharacters(resultStr)
		
		// Use xmlSlurper to parse xml
		result = new XmlSlurper().parseText(resultStr)
		
		// Extract artifact attributes
		result.children().each { artifactNode ->
			parseTopLevelAttributes(artifactNode, in_artifact)
			if (cacheLinks) parseLinksFromArtifactNode(artifactNode, in_artifact)
			artifactNode.children().each { child ->
				String iName = child.name()
				if (iName == "collaboration" ) {
					// Set artifact type and attributes
					String artifactType = parseCollaborationAttributes( child, in_artifact.attributeMap)
					in_artifact.setArtifactType(artifactType)
				}
				else if (iName == "content") {
					// Set primary text
					def richTextBody = child.text.richTextBody
					String primaryTextString = new groovy.xml.StreamingMarkupBuilder().bind {mkp.yield richTextBody.children() }
					in_artifact.setDescription(primaryTextString)

					if (includeCollections && primaryTextString.indexOf('WrapperResource')==-1) {
						// Check to see if this artifact has an embedded collection is "maximised" mode
						def memberHrefs = []
						if ( primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator maximised') > -1 ||
							 primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator showContent') > -1) {
							memberHrefs = parseCollectionHrefs(richTextBody)
							in_artifact.setDescription('') // Remove description, since it is now redundant
						}
						else {
							// Check to see if this artifact has an embedded collection in "minimized" mode
							def collectionIndex = primaryTextString.indexOf('com-ibm-rdm-editor-EmbeddedResourceDecorator minimised')
							if (collectionIndex > -1) { // Minimized Collection, get href for the collection artifact
								String hrefCollection = parseHref(primaryTextString.substring(collectionIndex))
								memberHrefs = getCollectionMemberHrefs(hrefCollection)
							}
							else if (in_artifact.getArtifactType() == 'Screen Field Configuration' || in_artifact.getArtifactType() == 'Screen Requirement') {
								//Check to see if this is a screen field config artifact and it has a link to a collection
								def collectionLnkIndex = primaryTextString.indexOf('<h:a')
								if ( collectionLnkIndex > -1) { // Link to Collection
									String hrefCollection = parseHref(primaryTextString.substring(collectionLnkIndex))
									memberHrefs = getCollectionMemberHrefs(hrefCollection)
		
								}
							}
						}
						
						// If there are collection members to be retrieved, then retrieve them
						if (memberHrefs) {
							getCollectionArtifacts(in_artifact, memberHrefs, cacheLinks)
						}
					}
				}
			}
		}
		return in_artifact

	}
	private String fixSpecialCharacters(String xml) {
		// Replace special characters for single/double quotes, dashes and trash characters
		return xml.replaceAll('Ã¢&#128;&#15(2|3);',"'").replaceAll('â&#128;&#15(2|3);', "'").replaceAll('â&#15(2|3);',"'").replaceAll('Ã&#131;Â¢&#128;&#15(2|3);',"'").replaceAll('&#128;&#15(2|3);',"'").replaceAll('â&#128;&#15(6|7);','&quot;').replace('â&#128;&#147;','-').replace('Ã&#131;Â¢&#128;&#147;','-').replace('Ã&#131;&#130;Ã&#130;', '').replace('&#128;',"'").replace('Â ', ' ').replace('Â ', ' ').replace('Â', '')
	}
	private String parseHref(String inString) {
		def hrefIndex = inString.indexOf('href=')
		String href = inString.substring(hrefIndex + 6)
		def endIndex = href.indexOf("'")
		return href.substring(0,endIndex)
	}
	private def parseCollectionHrefs(def richTextBody) {
		def memberHrefs = []
		def hrefs = richTextBody.'**'.findAll { p ->
			String src = p.@href
			"${p.name()}" == 'a' && "${src}".startsWith(this.clmUrl)
		}
		hrefs.each { href ->
			memberHrefs << "${href.@href}"
		}
		return memberHrefs
	}
	def getNonTextArtifact(def in_artifact, boolean includeCollections, boolean cacheLinks) {
		//log.debug("fetching non-text artifact")
		def result = rmGenericRestClient.get(
				uri: in_artifact.getAbout().replace("resources/", "publish/resources?resourceURI="),
				headers: [Accept: 'application/xml'] );
					
		// Should only be on artfact node
		def artifactNode = result.children()[0]
		
		// First validate that this is not a text format (which could happen if a WrapperResource is embedded in a text artifact)
		if ("${artifactNode.format}" == "Text") {
			return getTextArtifact(in_artifact, includeCollections, cacheLinks)
		}
		
		// Parse artifact attributes
		parseTopLevelAttributes(artifactNode, in_artifact)
		if (cacheLinks) parseLinksFromArtifactNode(artifactNode, in_artifact)
		artifactNode.children().each { child ->
			String iName = child.name()
			if (iName == "collaboration" ) {
				// Set artifact type and attributes
				String artifactType = parseCollaborationAttributes( child, in_artifact.attributeMap)
				in_artifact.setArtifactType(artifactType)
			}
			else if (iName == "wrappedResourceURI") {
				// Set primary text
				String primaryText = "<div>Uploaded Attachment</div>"
				in_artifact.setDescription(primaryText)
				String hRef = "${child}"
				in_artifact.setFileHref(hRef)
			}
		}
		
		
		return in_artifact

	}
	private def getCollectionMemberHrefs(String collectionHref) {
		def result = rmGenericRestClient.get(
				uri: collectionHref.replace("resources/", "publish/collections?resourceURI="),
				headers: [Accept: 'application/xml'] );
		
		def memberHrefs = []
		def links = result.'**'.findAll { p ->
			"${p.name()}" == 'Link' && "${p.@type}" == 'Link' && p.title == 'Unknown Link Type'
		}
		links.each { link ->
			memberHrefs.add("${link.relation}")
		}
		
		return memberHrefs
	}
	private def getCollectionArtifacts(def in_artifact, def memberHrefs, boolean cacheLinks) {
		memberHrefs.each { memberHref ->
			def artifact = new ClmModuleElement(null,in_artifact.getDepth()+1,null,'false',memberHref)

			in_artifact.collectionArtifacts << getTextArtifact(artifact,false,cacheLinks)
		}
	}
	
	public def getLinkInfoFromCache(def sid) {
		if (!sid) {
			return null
		}
		return cacheManagementService.getFromCache(sid, 'RM','LinkInfo')
	}
	
	void getWILinkChanges(int id, String project, Closure closure) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
//		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
//		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
//		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
//		Date modified = workItem.modified()
		String sid = "${id}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			List<LinkInfo> info = getLinkInfoFromCache(sid)
			if (info) {
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			wiData = generateWILinkChanges(wiData, info, cacheWI)
			if (wiData.body.size() > 1) {
				closure.call('WorkItem', wiData)
			}
			} else {
				//log.debug("No links for ${sid}")
			}

		}
	}
		
	def generateWILinkChanges(def wiData, links, cacheWI) {
			//def linksList = links.split(',')
			links = getLinks(links)
			links.each { LinkInfo info ->
				String id = info.itemIdRelated
				String module = info.moduleRelated
				def url = null
				//def linkMap = linkMapping[info.type]
				def linkType = info.type
				def linkMap = 'System.LinkTypes.Related'
				if (linkType == 'Interface For') {
					linkMap = 'System.LinkTypes.Hierarchy-Forward'
				}
				if (linkType == 'Interface') {
					linkMap = 'System.LinkTypes.Hierarchy-Reverse'
				}
				def runId = null
				def linkId = null
				if (linkMap) {
					if (module == 'rm') {
						def linkWI = cacheManagementService.getFromCache(id, 'RM', ICacheManagementService.WI_DATA)
						if (linkWI) {
							linkId = linkWI.id
							url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
						}
					}
					if (linkId && !linkExists(cacheWI, linkMap, linkId, runId) && "${linkId}" != "${cacheWI.id}") {
						def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap}", url: url, attributes:[comment: "DNG Link: ${linkType}"]]]
						wiData.body.add(change)
					}
				}
			}
			return wiData
		}
	
		/**
		 * Check work item cache to see if link exists on work item.
		 *
		 * @param cacheWI
		 * @param targetName
		 * @param linkId
		 * @return
		 */
		boolean linkExists(cacheWI, targetName, linkId, String runId = null) {
			def url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
			if (runId) {
				url = "${tfsUrl}/_TestManagement/Runs?_a=resultSummary&runId=${runId}&resultId=${linkId}"
			}
			def link = cacheWI.relations.find { rel ->
				"${rel.rel}" == "${targetName}" && url == "${rel.url}"
			}
			return link != null
		}
	

		List<LinkInfo> getLinks(links) {
			List<LinkInfo> linkinfos = new ArrayList<LinkInfo>()
			links.findAll { link ->
				def info = new LinkInfo(type: "${link.type}", itemIdCurrent: "${link.itemIdCurrent}", itemIdRelated: "${link.itemIdRelated}", moduleCurrent: "${link.moduleCurrent}", moduleRelated: "${link.moduleRelated}")
				if (info.getModuleRelated() == 'rm') {
					linkinfos.add(info)
				}
			}
			return linkinfos
		}
	
		/*
		 * bout to get mad silly
		 * We go through all LinkInfo objects just to get their parent id
		 */
	def processLinks() {
		if (cacheManagementService instanceof MongoDBCacheManagementService) {
			MongoDBCacheManagementService mdbCacheManagement = cacheManagementService
			int page = 0
			while (true) {
				def linkinfos = mdbCacheManagement.getAllOfType('LinkInfo', page)
				if (linkinfos.size() == 0) break
				linkinfos.findAll { link ->
					def cid = "${link.itemIdCurrent}"
					List<LinkInfo> links = getLinks(linkinfos)
					def cacheWI = cacheManagementService.getFromCache(cid, 'RM', 'wiData')
					def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
					def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
					wiData.body.add(rev)
					wiData = generateWILinkChanges(wiData, links, cacheWI)
				}
				page++
			}
		} else {
			def cacheWIs = cacheManagementService.getAllOfType('wiData')
		}
	}

	private void parseLinksFromArtifactNode(def artifactNode, def in_artifact) {
		String modified = artifactNode.collaboration.modified
		String identifier = artifactNode.identifier
		Date modifiedDate = Date.parse("yyyy-MM-dd'T'hh:mm:ss",modified)
		//log.debug("Attempting to find and cache links for ${identifier}")
		in_artifact.setLinks(getAllLinks(identifier, modifiedDate, artifactNode))
	}
	
	private void parseTopLevelAttributes(def artifactNode, def in_artifact) {
		in_artifact.setFormat("${artifactNode.format}")
		
		String title = "${artifactNode.title}"
		if (title == '') {
			title= "${artifactNode.description}"
		}
		//amending this to deal with null titles in addition to blanks
		if (!title) {
			title= "<blank title>"
		}
		
		//amending this to deal with invalid ascii
		String identifier = "${artifactNode.identifier}"
		title = Normalizer.normalize(title,Normalizer.Form.NFKD)
		title = title.replaceAll("[^\\p{ASCII}]", "") //ascii titles only
		in_artifact.setTitle(title)
		
		if (in_artifact.getBaseArtifactURI() == null || in_artifact.getBaseArtifactURI() == ''){
			String core = "${artifactNode.core}"
			if (core == null || core == '') {
				in_artifact.setBaseArtifactURI(in_artifact.getAbout())
			}
			else {
				// then this is a module element and we need to set the base
				in_artifact.setBaseArtifactURI("${this.rmBGenericRestClient.clmUrl}","${core}")
			}
		}
	}
	
	private String parseCollaborationAttributes(NodeChild in_rootCollaborationNode, Map out_attributeMap ) {
		// Declare type as return argument
		String artifactType
		
		// Extract artifact attributes
		in_rootCollaborationNode.children().each { attr ->
			String attrName = attr.name()
			if (attrName == "creator") {
				String creatorURI = attr.about
				out_attributeMap.put(attrName, creatorURI)
			}
			else if (attrName == "created") {
				String creationDate = attr
				out_attributeMap.put(attrName, creationDate)
			}
			else if (attrName == "attributes") {
				// Get artifact type
				def objTypeAttr = attr.objectType[0].nodeIterator().next()?.attributes()
				artifactType = objTypeAttr['{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name']
				
				// Get custom attributes
				String multiValueAttrName = null
				String multiValueAttrVal = null
				attr.objectType.children().each { custAttr ->
					def attributes = custAttr.nodeIterator().next()?.attributes()
					String custAttrName =  attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}name"]
					String custAttrIsEnum = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}isEnumeration"]
					String custAttrIsMultiValued = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}isMultiValued"]
					String custAttrLiteralName = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}literalName"]
					String custAttrValue = attributes["{http://jazz.net/xmlns/alm/rm/attribute/v0.1}value"]
					String custAttrVal
					if (custAttrIsEnum == "true") {
						 custAttrVal = custAttrLiteralName
					}
					else {
						 custAttrVal = custAttrValue
					}
					if (custAttrIsMultiValued == 'true') {
						if (multiValueAttrName == null) { // First instance of this attribute
							multiValueAttrName = "${custAttrName}"
							multiValueAttrVal = "${custAttrVal}"
						}
						else if (multiValueAttrName == custAttrName){ // More values for same attribute
							multiValueAttrVal = multiValueAttrVal + ';' + "${custAttrVal}"
						}
						else { // New multivalue attr with previous one to save
							out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
							multiValueAttrName = "${custAttrName}"
							multiValueAttrVal = "${custAttrVal}"
						}
					}
					else { // single value attribute
						if (multiValueAttrName != null) {  // Save pending multivalue attribute
							out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
							multiValueAttrName = null
							multiValueAttrVal = null
						}
						out_attributeMap.put(custAttrName, custAttrVal)
						
					}
				}
				// Add any pending multiValue attribute
				if (multiValueAttrName != null) {  // Save pending multivalue attribute
					out_attributeMap.put(multiValueAttrName, multiValueAttrVal)
				}

			}
		}
		
		return artifactType

	}
	
	// Get Attachment content (binary) from URI
	def getContent(String uri) {
		def result = rmGenericRestClient.get(
			withHeader: true,
			uri: uri,
			contentType: ContentType.BINARY
			);
		return result
	}
	
	//what we need: artifact id and type, then the link set to look through and get all links
	//if how we get the type varies we can pass that in from the parent I suppose
	//if we use datemodified as a parameter as well we can use that to set the cache date
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, def artifactNode) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()

		artifactNode.traceability.links.children().each { link ->
			//String itemIdCurrent = child.name()
			String rid = link.identifier //if the target is QM this will be a guid
			String key = link.title //if we just want the string type of link it's .title, uri to type is .linkType
			String module = link.relation.text().split('/')[3]
			//log.debug("Found link for Artifact ${id} to ${module} item ${rid}")0
			if (module == 'qm') {
				rid = link.alternative
			}
			def info = new LinkInfo(type: key, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'rm', moduleRelated: module)
			links.add(info)
		}
		if (links.size() > 0) {
				cacheManagementService.saveToCache(links, id, 'LinkInfo')
		}
		return links
	}
}

//This class is used by the CacheInterceptor to store the direct query results; there are similar identical classes for both CCM and QM.
//I am unsure if the classname is why they are different and that has some impact on how the data is stored in the cache,
//but for consistancy's sake we are making a new data class in the same manner as ClmTestManagementService
@Slf4j
class RequirementQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		log.debug("ReqQueryData serializing result doData")
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		log.debug("ReqQueryData returning serialized result dataValue")
		return new XmlSlurper().parseText(data)
	}
}

@Slf4j
class DataWarehouseQueryData implements CacheWData {
	def data
	
	void doData(def result) {
		//log.debug("Saving new DataWarehouseQueryData page to mongodb")
		data = new JsonBuilder(result).toPrettyString()
	}
	
	def dataValue() {
		//log.debug("DWQueryData returning serialized result dataValue")
		return new JsonSlurper().parseText(data)
	}
}

@Slf4j
class SqlLoader {
	
	String sqlQuery(String sqlresource) {
		log.debug("Sql file at ${sqlresource}")
		if (sqlresource.startsWith('/')) sqlresource=sqlresource.substring(1)
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(sqlresource)
		//File sqlFile = new File(url.file)
		String sql = is.text
		log.debug("Retrieved SQL query: ${sql}")
		return sql
	}
}