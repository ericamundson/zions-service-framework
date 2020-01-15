package com.zions.clm.services.ccm.workitem

import java.util.Map

import org.eclipse.core.runtime.IProgressMonitor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.registry.IEndPointDescriptor
import com.ibm.team.process.client.IProcessClientService
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.common.IContributor
import com.ibm.team.repository.common.TeamRepositoryException
import com.ibm.team.workitem.client.IQueryClient
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.AttributeTypes
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.IWorkItemReferences
import com.ibm.team.workitem.common.model.ItemProfile
import com.ibm.team.workitem.common.query.IQueryDescriptor
import com.ibm.team.workitem.common.query.IQueryResult
import com.ibm.team.workitem.common.query.IResolvedResult
import com.ibm.team.workitem.common.query.QueryTypes
import com.ibm.team.workitem.common.query.ResultSize
import com.zions.clm.services.ccm.client.CcmGenericRestClient
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.utils.ProcessAreaUtil
import com.zions.clm.services.ccm.utils.ReferenceUtil
import com.zions.clm.services.ccm.workitem.handler.CcmBaseAttributeHandler
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.ldap.User
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.user.UserManagementService
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * Provides behavior to process RTC work items in a form to be used to translate to VSTS work items.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class CcmWorkManagementService {
	
	@Autowired(required=false)
	private Map<String, CcmBaseAttributeHandler> fieldMap;
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	ICacheManagementService cacheManagementService
	

	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired(required=false)
	CcmGenericRestClient ccmGenericRestClient
	
	@Autowired(required=false)
	UserManagementService userManagementService
	
	IQueryResult<IResolvedResult> resolvedResults = null
	//def queryResults = null
	
	List<String> queryList = null
	String projectName
	int queryIndex = 0
	String queryName = 'none'
	int pageIndex = 0
	
	Map<String, String> itemMap = ['testcase': 'Test Case', 'testsuite': 'Test Suite', 'testplan': 'Test Case', 'executionresult': 'Result']
	

	int newId = -1
	
	public CcmWorkManagementService() {
		
	}
	
	def resetNewId() {
		newId = -1
	}
	
	def getWorkitem(id) {
		if (id instanceof String) {
			id = Integer.parseInt(id)
		}
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		return workItem
	}
	
	/**
	 * Get the data structure of field changes to create/update VSTS work item.
	 * 
	 * @param id
	 * @param project
	 * @param translateMapping
	 * @param memberMap
	 * @return
	 */
	def getWIChanges(id, project, translateMapping, memberMap) {
		IWorkItem workItem = getWorkitem(id)
		String type = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), 'workItemType')
		def wiMap = translateMapping["${type}"]
		if (wiMap == null) {
			log.error("Map for work item type not found:  ${type}")
			return null
		}
		def outType = "${wiMap.target}"
		return generateWIData(id, workItem.modified(), workItem,  project, outType, wiMap, memberMap).changes
	}
	
	/**
	 * Returns link changes from RTC that is in object usable for VSTS work item update request.
	 * 
	 * @param id
	 * @param project
	 * @param linkMapping
	 * @return
	 */
	void getWILinkChanges(int id, String project, linkMapping, Closure closure) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		Date modified = workItem.modified()
		String sid = "${id}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			List<LinkInfo> info = this.getAllLinks(sid, modified, workItem, linkMapping)
			def resultLinks = getLinks('affects_execution_result',info)
			resultLinks.each { LinkInfo link ->
				def result = cacheManagementService.getFromCache(link.itemIdRelated, 'QM', ICacheManagementService.RESULT_DATA)
				if (result && result.id) {
					//log.info("Has execution result: ${result.id}, key: ${link.itemIdRelated}")
					String title = "${result.testCaseTitle}"
					
					def resultChanges = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${result.testRun.id}/results", query:['api-version':'5.0'], body: []]
					def data = [id: result.id, testCaseTitle: title, associatedBugs: []]
					def wis = []
					if (result.associatedBugs) {
						result.associatedBugs.each { bug ->
							String bid = "${bug.id}"
							wis.add(bid)
						}
						data.associatedBugs.addAll(result.associatedBugs)
					}
					String wid = "${cid}"
					if (!wis.contains(wid)) {
						data.associatedBugs.add([id:wid])
						resultChanges.body.add(data)
						def changes = [resultChanges: resultChanges, rid: link.itemIdRelated]
						closure.call('Result', changes)
					}
				}
			}
			
			if (resultLinks.size() > 0) {
				cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
			}
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			wiData = generateWILinkChanges(wiData, info, linkMapping, cacheWI)
			if (wiData.body.size() > 1) {
				closure.call('WorkItem', wiData)
			}

		}
	}
	
	List<LinkInfo> getLinks(String type, links) {
		List<LinkInfo> out = links.findAll { LinkInfo link ->
			link.type == type
		}
		return out
	}
	
	/**
	 * Generates work item link changes.
	 * 
	 * @param wiData
	 * @param linkIds
	 * @param key
	 * @param linkMap
	 * @param cacheWI
	 * @return
	 */
	def generateWILinkChanges(def wiData, List<LinkInfo> links, linkMapping, cacheWI) {
		//def linksList = links.split(',')
		links.each { LinkInfo info -> 
			String id = info.itemIdRelated
			String module = info.moduleRelated
			def url = null
			def linkMap = linkMapping[info.type]
			def runId = null
			def linkId = null
			if (linkMap) {
				if (info.type != 'affects_execution_result') {
					def linkWI = cacheManagementService.getFromCache(id, module, ICacheManagementService.WI_DATA)
					if (linkWI) {
						linkId = linkWI.id
						url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
					}
				}
				
				def link = getLink(cacheWI, "${linkMap.@target}", linkId, wiData, runId)
			
				if (linkId && !link.link && "${linkId}" != "${cacheWI.id}") {
					def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap.@target}", url: url, attributes:[comment: "${linkMap.@source}"]]]
					wiData.body.add(change)
				} 
//				else if (link.link && link.index > -1) {  // Ensure parent
//					String lRelation = link.link.rel
//					String lTarget = "${linkMap.@target}"
//					if (lTarget == 'System.LinkTypes.Hierarchy-Reverse' && lRelation != 'System.LinkTypes.Hierarchy-Reverse') {
//						def rChange = [op: 'remove', path: "/relations/${link.index}"]
//						wiData.body.add(rChange)
//						def pChange = [op: 'add', path:'/relations/-', value: [rel:"${linkMap.@target}", url: url, attributes:[comment: "${linkMap.@source}"]]]
//						wiData.body.add(pChange)
//						log.info("Ensuring parent relation for work item:  ${cacheWI.id}")
//					}
//				}
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
	def getLink(cacheWI, String targetName, linkId, wiData, String runId = null) {
		if (!linkId) return [link: null, index: -1]
		//String url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
		String lid = "/${linkId}"
		if (runId) {
			//url = "${tfsUrl}/_TestManagement/Runs?_a=resultSummary&runId=${runId}&resultId=${linkId}"
			lid = ".${linkId}"
		}
		def wlink = wiData.body.find { change ->
			String eurl = ''
			String erel = ''
			if (change.value && !(change.value instanceof Integer) && change.value.url) {
				eurl = "${change.value.url}"
				erel = "${change.value.rel}"
			}
			eurl.endsWith(lid) && erel == targetName
		}
		if (wlink != null) return [link: wlink, index: -1]
		int i = 0
		def link = null
		def index = 0
		//cacheWI.relations.each { rel ->
		for (rel in cacheWI.relations) {
			String eUrl = "${rel.url}"
			String eRel = "${rel.rel}"
			if (eUrl.endsWith(lid) && eRel == targetName) {
				link = rel
				index = i
				break;
			}
			i++
			
		}
		return [link: link, index: index]
	}
	
	boolean canChange(prevWI, cacheWI, field, String key) {
		if (!cacheWI) return true
		boolean flag = true
		String tName = "${field.target}"
		def fModified = cacheManagementService.getFromCache("${key}-${tName}", 'changedField')
		if (fModified) {
			def cVal = cacheWI.fields."${tName}"
			String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
			return false
		}
		if (!prevWI) return true
		def cVal = cacheWI.fields."${tName}"
		String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
		def pVal = prevWI.fields."${tName}"
		flag = "${pVal}" == "${cVal}"
		if (!flag) {
			log.info("ADO field change cached:  key: ${key}-${tName}, date: ${changedDate}, value: ${cVal}.")
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
		}
		return flag
	}


	/**
	 * Generate work item VSTS change request from RTC datat.
	 * 
	 * @param workItem
	 * @param id
	 * @param project
	 * @param type
	 * @param wiMap
	 * @param memberMap
	 * @return
	 */
	//@Cache( elementType = WorkitemChanges)
	WorkitemChanges generateWIData(id, Date timeStamp, workItem,  project, type, wiMap, memberMap) {
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		String sid = "${id}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		def prevWI = cacheManagementService.getFromCache(sid, 'wiPrevious')
		if (cacheWI != null) {
			def cid = cacheWI.id
			wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
		} else {
			def idData = [ op: 'add', path: '/id', value: newId]
			newId--
			wiData.body.add(idData)
		}
		wiMap.fieldMaps.each { fieldMap -> 
			if (canChange(prevWI, cacheWI,  fieldMap, sid)) {
				def fieldData = getFieldData(workItem, fieldMap, cacheWI, memberMap, wiMap)
				if (fieldData != null) {
					if (!(fieldData instanceof ArrayList)) {
						wiData.body.add(fieldData)
					} else {
						fieldData.each { fData ->
							if (fData.value != null) {
								wiData.body.add(fData)
							}
						}
					}
				}
			}
			
		}
		if (wiData.body.size() == 1) {
			return null
		}
		//String json = new JsonBuilder(wiData).toPrettyString()
		WorkitemChanges data = new WorkitemChanges(changes: wiData)
		return data
	}
	
	User getUserByEmail(String email) {
		try {
			return userManagementService.getUserByEmail(email)
		} catch (e) {}
		return null
	}
		
	/**
	 * Get field specific change data.
	 * 
	 * @param workItem
	 * @param fieldMap
	 * @param cacheWI
	 * @param memberMap
	 * @return
	 */
	def getFieldData(IWorkItem workItem, def fieldMap, def cacheWI, memberMap, wiMap) {
		String attributId = "${fieldMap.source}"
		String ccmHandlerName = "Ccm${attributId.substring(0,1).toUpperCase()}${attributId.substring(1)}"
		String fValue = ""
		if (attributId.trim().equals("")) {
			fValue = "any"
		} else {
			fValue = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), attributId)
			if (fValue == null && this.fieldMap[ccmHandlerName] != null) {
				def data = [workItem: workItem, memberMap: memberMap, fieldMap: fieldMap, cacheWI: cacheWI, wiMap: wiMap]
				return this.fieldMap[ccmHandlerName].execute(data)
			} else if (fValue == null && this.fieldMap["${attributId}"] != null) {
				def data = [workItem: workItem, memberMap: memberMap, fieldMap: fieldMap, cacheWI: cacheWI, wiMap: wiMap]
				return this.fieldMap["${attributId}"].execute(data)
			}
			
			

			if (fValue == null) return null
		}
		
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)		
		IAttribute attribute = workItemClient.findAttribute(workItem.getProjectArea(), attributId, null);
		if (attribute != null) {
			String attribType = attribute.getAttributeType()
			User user = null
			if (attribType.equals(AttributeTypes.CONTRIBUTOR)) {
				user = getUserByEmail(fValue)
			}
			if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] != null) {
				fValue = "${memberMap[fValue.toLowerCase()].uniqueName}"
			} else if (user) {
				fValue = "${user.email.toLowerCase()}"
			}
			if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] == null && !user) {
				return null
			}
		}
		String cValue = ""
		if (cacheWI != null) {
			cValue = "${cacheWI.fields["${fieldMap.target}"]}"
			def val = "${fValue}"
			if (fieldMap.defaultMap != null) {
				val = fieldMap.defaultMap.target
			}
			if (fieldMap.valueMap.size() > 0) {
				
				fieldMap.valueMap.each { aval ->
					if ("${fValue}" == "${aval.source}") {
						val = "${aval.target}"
						return
					}
				}
			}
			if ("${val}" != 'skip') {
				if ("${fieldMap.outType}" == 'integer') {
					val = Integer.parseInt(val)
				} else if ("${fieldMap.outType}" == 'double') {
					val = Double.parseDouble(val)
				} else if ("${fieldMap.outType}" == 'boolean') {
					val = Boolean.parseBoolean(val)
				} else if ("${fieldMap.outType}" == 'string' && "${val}".length() > 255) {
					val = 	"${val}".substring(0, 255-1)
				}
			}
			if ("${val}" != "${cValue}") {
				if ("${val}" == 'skip') {
					return [op:'add', path: "/fields/${fieldMap.target}", value: '']
				}
				return [op:'add', path: "/fields/${fieldMap.target}", value: val]
			} else {
				return null
			}
		}
		def val = "${fValue}"
		if (fieldMap.defaultMap != null) {
			val = fieldMap.defaultMap.target
		}
		if (fieldMap.valueMap.size() > 0) {
			
			fieldMap.valueMap.each { aval ->
				if ("${fValue}" == "${aval.source}") {
					val = "${aval.target}"
					return
				}
			}
		}
		if ("${val}" != 'skip') {
			if ("${fieldMap.outType}" == 'integer') {
				val = Integer.parseInt(val)
			} else if ("${fieldMap.outType}" == 'double') {
				val = Double.parseDouble(val)
			} else if ("${fieldMap.outType}" == 'boolean') {
				val = Boolean.parseBoolean(val)
			} else if ("${fieldMap.outType}" == 'string' && "${val}".length() > 255) {
				val = 	"${val}".substring(0, 255-1)

			}
		}
		if ("${val}" == 'skip') {
			return [op:'add', path: "/fields/${fieldMap.target}", value: '']
		}
		return [op:'add', path:"/fields/${fieldMap.target}", value: val]
		
	}
	
	IWorkItemCommon getWorkItemCommon() {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		return teamRepository.getClientLibrary(IWorkItemCommon.class)
	}


	@Cache(elementType = LinkInfo)
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, workItem, linkMapping) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()
		linkMapping.each { key, linkMap ->
			String linkType = ReferenceUtil.getReferenceType(key);
			String module = 'CCM'
			if ("${linkMap.@module}".length() > 0) {
				module = "${linkMap.@module}"
			}
			String ids = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), "${key}")
			String[] idList = ids.split(',')
			idList.each { String rid ->
				if (rid && rid.length() > 0) {
					rid = resolveId(rid, module)
					if (rid && "${linkMap.@key}".length() > 0) {
						rid = "${rid}-${linkMap.@key}"
					}
					//log.info("Related ID for work item (${id}):  ${module} ${rid}")
					if (rid != null) {
						def info = new LinkInfo(type: key, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'CCM', moduleRelated: module)				
						links.add(info)
					}
				}
			}

		}
		return links
	}
	
	String resolveId(String cId, String module) {
		String id = null
		if (cId.startsWith('http')) {
			try {
				String url = cId
				def result = ccmGenericRestClient.get(
				contentType: ContentType.XML,
				uri: cId,
				headers: [Accept: 'application/rdf+xml'] );
				if (!result) {
					return null
				}
				if (module == 'QM') {
					def identifier = result.'**'.find { node ->
				
						node.name() == 'shortId'
					}
//					String xml = new XmlUtil().serialize(result)
//					File outXml = new File('clm.xml')
//					def os = outXml.newDataOutputStream()
//					os << xml
//					os.close()
					id = "${identifier.text()}"
				} else if (module == 'RM') {
					def identifier = result.'**'.find { node ->
				
						node.name() == 'identifier'
					}
//					String xml = new XmlUtil().serialize(result)
//					File outXml = new File('clm.xml')
//					def os = outXml.newDataOutputStream()
//					os << xml
//					os.close()

					id = "${identifier.text()}"
				}
			} catch (e) {}
		} else {
			id = cId
		}
		return id
	}
	
	IProgressMonitor getMonitor() {
		return rtcRepositoryClient.getMonitor()
	}

	public static IQueryDescriptor findSharedQuery(	IProjectArea projectArea,
			List sharingTargets, String queryName,  IProgressMonitor monitor)
			throws TeamRepositoryException {
		// Get the required client libraries
				QueryTypes t
		ITeamRepository teamRepository = (ITeamRepository)projectArea.getOrigin();
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);
		IQueryClient queryClient = workItemClient.getQueryClient();
		IQueryDescriptor queryToRun = null;
		List queries = queryClient.findSharedQueries(projectArea.getProjectArea(),
			sharingTargets, QueryTypes.WORK_ITEM_QUERY,
		IQueryDescriptor.FULL_PROFILE, monitor);
		// Find a query with a matching name
		for (Iterator iterator = queries.iterator(); iterator.hasNext();) {
			IQueryDescriptor iQueryDescriptor = (IQueryDescriptor) iterator.next();
			if (iQueryDescriptor.getName().equals(queryName)) {
				queryToRun = iQueryDescriptor;
				break;
			}
		}
		return queryToRun;
	}
	
	public static IQueryDescriptor findPersonalQuery(IProjectArea projectArea,
		String queryName, IProgressMonitor monitor)
		throws TeamRepositoryException {
		// Get the required client libraries
		ITeamRepository teamRepository = (ITeamRepository)projectArea.getOrigin();
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(
			IWorkItemClient.class);
		IQueryClient queryClient = workItemClient.getQueryClient();
		// Get the current user.
		IContributor loggedIn = teamRepository.loggedInContributor();
		IQueryDescriptor queryToRun = null;
		// Get all queries of the user in this project area.
		List queries = queryClient.findPersonalQueries(
			projectArea.getProjectArea(), loggedIn,
			QueryTypes.WORK_ITEM_QUERY,
			IQueryDescriptor.FULL_PROFILE, monitor);
		// Find a query with a matching name
		for (Iterator iterator = queries.iterator(); iterator.hasNext();) {
			IQueryDescriptor iQueryDescriptor = (IQueryDescriptor) iterator.next();
			if (iQueryDescriptor.getName().equals(queryName)) {
				queryToRun = iQueryDescriptor;
				break;
			}
		}
		return queryToRun;
	}
	
	public def multiQuery(List<String> queryList, String projectName) {
		this.queryList = queryList
		this.projectName = projectName
		this.resolvedResults = null
		this.pageIndex = 0
		this.queryName = 'none'
		this.queryIndex = 0
	}
	
	public def multiNext() {
		def retVal = null
		if (!resolvedResults) {
			queryName = queryList.get(queryIndex)
			retVal = runQuery(queryName, projectName)
			queryIndex++
		} else {
			retVal = nextPage()
			pageIndex++
			if (!retVal && queryIndex < queryList.size()) {
				queryName = queryList.get(queryIndex)
				retVal = runQuery(queryName, projectName)
				queryIndex++
				pageIndex = 0
			}
		}
		return retVal
	}
	
	public String multiPageUrl() {
		return "${queryName}-${pageIndex}"
	}
	
	public def runQuery(String queryName, String projectName) {
		//ProcessAreaUtil u
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IProcessClientService clientService = teamRepository.getClientLibrary(IProcessClientService.class)
		IProjectArea projectArea = ProcessAreaUtil.findProjectAreaByFQN(projectName, clientService, getMonitor())
		List sharingTargets = new ArrayList();
		// Add desired sharing targets
		sharingTargets.add(projectArea);
		IQueryDescriptor sharedQuery = findPersonalQuery(projectArea, queryName, getMonitor());
		
//		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);
//		IQueryClient queryClient = workItemClient.getQueryClient();
//		IQueryResult unresolvedResults = queryClient.getQueryResults(sharedQuery);
		
		IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);
		IQueryClient queryClient = workItemClient.getQueryClient();
		// Set the load profile
		ItemProfile loadProfile = IWorkItem.SMALL_PROFILE;
		resolvedResults = queryClient.getResolvedQueryResults(sharedQuery, loadProfile)
		resolvedResults.setLimit(Integer.MAX_VALUE)
		ResultSize limit = resolvedResults.getResultSize(getMonitor())
	
		resolvedResults.setPageSize(200)
		
		return nextPage()
	}
	
	public def nextPage() {
		def wil = []
		if (resolvedResults.hasNext(null)) {
			List<IResolvedResult> page = resolvedResults.nextPage(null)
			for (IResolvedResult r: page) {
				IWorkItem workItem = r.getItem();
				String summary = workItem.getHTMLSummary().getPlainText()
				Date modified = workItem.modified()
				String mDateStr = modified.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				def wiOut = [id: "${workItem.id}", summary: summary, modified: mDateStr]
				wil.add(wiOut)
				
			}
			return wil
		}
		return null
		
	}
}


