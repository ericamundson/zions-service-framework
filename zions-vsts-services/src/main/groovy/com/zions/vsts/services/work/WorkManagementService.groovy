package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Manages VSTS interaction to create, update and delete work items.
 * <ul>
 * <li>
 * Submits batch create/update requests
 * </li>
 * <li>
 * Manages caching of ADO work items to optimize requests made to ADO.
 * </li>
 * <li>Handles refresh of work item cache from imported work items query.</li>
 * <li>Manages cleanup of a work item import via a specified query.</li>
 * </ul>
 * @author z091182
 *
 */
@Component
@Slf4j
class WorkManagementService {

	final private int BATCHSIZE = 200

	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	@Autowired(required=false)
	ICacheManagementService cacheManagementService

	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService

	@Value('${id.tracking.field:}')
	private String idTrackingField
	
	@Value('${pageSize:200}')
	private int pageSize
	
	@Value('${track.ado.changes:false}')
	boolean trackAdoChanges
	
	@Value('${error.caching:false}')
	boolean errorCaching

	private categoriesMap = [:]

	public WorkManagementService() {
	}
	
	public def cleanBadResultLinks(String collection, String project, String teamArea) {
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [System.AreaPath] under '${teamArea}' AND [Custom.ExternalID] CONTAINS 'RQM-'"
		def wis = getWorkItems(collection, project, query)
		if (!wis || !wis.workItems) return
		def wiList = wis.workItems
		int j = 0
		//def eMap = [:]
		while (true) {
			def ids = []
			def keys = []
			for (int i = 0; i < 200 && j < wiList.size(); i++) {
				def wi = wiList[j]
				ids.push(wi.id)
				j++
			}
			def result = batchGet(collection, project, ids)
			result.value.each { owi ->
				String key = getKey(owi)
				def cacheWI = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
				String oid = "${owi.id}"
				String cid = "${cacheWI.id}"
				if (oid == cid) {
					def changes = genRemoveBadResultLinks(collection, project, owi)
				}
			}
		}
	}
	
	/**
	 * Bug fix for WI: 932997
	 *
	 * @param collection
	 * @param project
	 * @param teamArea
	 * @return
	 */
	public def fixTestCaseSteps(String collection, String project, String teamArea) {
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [System.WorkItemType] IN ('Test Case') AND [System.AreaPath] UNDER '${teamArea}' AND [Custom.ExternalID] Contains 'RQM-'"
		def wis = getWorkItems(collection, project, query)
		if (!wis || !wis.workItems) return
		def wiList = wis.workItems
		int j = 0
		//def eMap = [:]
		def clManager = new ChangeListManager(collection, project, this);
		while (true) {
			def ids = []
			for (int i = 0; i < 200 && j < wiList.size(); i++) {
				def wi = wiList[j]
				ids.push(wi.id)
				j++
			}
			def result = batchGet(collection, project, ids)
			result.value.each { owi ->
				def changes = generateStepsChanges(collection, project, owi)
				if (changes) {
					String key = getKey(owi)
					
					clManager.add(key, changes)
				}
			}
			if (j == wiList.size()) {
				def lwi = wiList[j - 1]
				String lastId =  "${lwi.id}"
				j = 0
				wis = getWorkItems(collection, project, query, lastId)
				if (!wis || !wis.workItems || !wis.workItems.size() == 0) return
				wiList = wis.workItems
			}
		}
		clManager.flush()
	}
	
	private def generateStepsChanges(String collection, String project, owi) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def cid = owi.id
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def rev = [ op: 'test', path: '/rev', value: owi.rev]
		wiData.body.add(rev)
		def sChanges = stepFieldChanges(owi)
		if (sChanges) {
			wiData.body.add(sChanges)
			return wiData
		}
		return null
	}
	
	private def stepFieldChanges(owi) {
		String steps = "${owi.fields.'Microsoft.VSTS.TCM.Steps'}}"
		// Address all double tags:  << blah blah >>
		String val = steps.replaceAll(/&lt;&lt;[^&]*?&gt;&gt;/) { it ->
							String s = it
							s = s.replace('&', '&amp;')
							s
						};
		// Address all tags with single digit or just empty tag
		val = val.replaceAll(/&lt;[ ]?[0-9]?[ ]?&gt;/) { it ->
							String s = it
							s = s.replace('&', '&amp;')
							s
						};
		// Address all tags that start with ... (ignore case)
		val = fixAllTagsStartWith('[\\$#"]', val)
		val = fixAllTagsStartWith('\\*', val)
		val = fixAllTagsStartWith('[0-4]', val)
		val = fixAllTagsStartWith('act[ui]', val)
		val = fixAllTagsStartWith('acc[ot]', val)
		val = fixAllTagsStartWith('affiliate', val)
		val = fixAllTagsStartWith('after', val)
		val = fixAllTagsStartWith('Amount', val)
		val = fixAllTagsStartWith('Annual', val)
		val = fixAllTagsStartWith('APR ', val)
		val = fixAllTagsStartWith('Appro', val)
		val = fixAllTagsStartWith('[/]?app', val)
		val = fixAllTagsStartWith('Archived', val)
		val = fixAllTagsStartWith('Auto ', val)
		val = fixAllTagsStartWith('Bill[ i]', val)
		val = fixAllTagsStartWith('blank', val)
		val = fixAllTagsStartWith('before', val)
		val = fixAllTagsStartWith('[/]?B[a]?NCS', val)
		val = fixAllTagsStartWith('borrower', val)
		val = fixAllTagsStartWith('bucket', val)
		val = fixAllTagsStartWith('Capital', val)
		val = fixAllTagsStartWith('Customer', val)
		val = fixAllTagsStartWith('Check', val)
		val = fixAllTagsStartWith('Cleanup', val)
		val = fixAllTagsStartWith('Common', val)
		val = fixAllTagsStartWith('Day', val)
		val = fixAllTagsStartWith('De[fs]', val)
		val = fixAllTagsStartWith('Due', val)
		val = fixAllTagsStartWith('dat[ae]', val)
		val = fixAllTagsStartWith('Draw ', val)
		val = fixAllTagsStartWith('Effective', val)
		val = fixAllTagsStartWith('End ', val)
		val = fixAllTagsStartWith('EO[YDM]', val)
		val = fixAllTagsStartWith('Expir', val)
		val = fixAllTagsStartWith('FEB[ -]', val)
		val = fixAllTagsStartWith('Frequency', val)
		val = fixAllTagsStartWith('[/]?fns[/]?', val)
		val = fixAllTagsStartWith('Fee', val)
		val = fixAllTagsStartWith('First ', val)
		val = fixAllTagsStartWith('Fu[nt]', val)
		val = fixAllTagsStartWith('general', val)
		val = fixAllTagsStartWith('Grace', val)
		val = fixAllTagsStartWith('HHMMSS', val)
		val = fixAllTagsStartWith('Ho[lw]', val)
		val = fixAllTagsStartWith('http:', val)
		val = fixAllTagsStartWith('IOF-', val)
		val = fixAllTagsStartWith('inter[fe]', val)
		val = fixAllTagsStartWith('implementation ', val)
		val = fixAllTagsStartWith('Job', val)
		val = fixAllTagsStartWith('Last ', val)
		val = fixAllTagsStartWith('MAR[C]? 2015', val)
		val = fixAllTagsStartWith('Maturity', val)
		val = fixAllTagsStartWith('mmfdmain', val)
		val = fixAllTagsStartWith('Next ', val)
		val = fixAllTagsStartWith('ODS', val)
		val = fixAllTagsStartWith('Other', val)
		val = fixAllTagsStartWith('option', val)
		val = fixAllTagsStartWith('lonq', val)
		val = fixAllTagsStartWith('On ', val)
		val = fixAllTagsStartWith('Payment', val)
		val = fixAllTagsStartWith('Path ', val)
		val = fixAllTagsStartWith('Periodic', val)
		val = fixAllTagsStartWith('Pre[ -]?condition', val)
		val = fixAllTagsStartWith('Pre[ -]?requisite', val)
		val = fixAllTagsStartWith('Post', val)
		val = fixAllTagsStartWith('Query', val)
		val = fixAllTagsStartWith('Run', val)
		val = fixAllTagsStartWith('Rate ', val)
		val = fixAllTagsStartWith('Requirement', val)
		val = fixAllTagsStartWith('Rep[ao]', val)
		val = fixAllTagsStartWith('Response', val)
		val = fixAllTagsStartWith('Remittance', val)
		val = fixAllTagsStartWith('Renew', val)
		val = fixAllTagsStartWith('Review', val)
		val = fixAllTagsStartWith('Rollover', val)
		val = fixAllTagsStartWith('Seasonal', val)
		val = fixAllTagsStartWith('Select', val)
		val = fixAllTagsStartWith('Settlement', val)
		val = fixAllTagsStartWith('Second ', val)
		val = fixAllTagsStartWith('SI_HOME', val)
		val = fixAllTagsStartWith('Step', val)
		val = fixAllTagsStartWith('[/]sch', val)
		val = fixAllTagsStartWith('Sl Nbr', val)
		val = fixAllTagsStartWith('Soap', val)
		val = fixAllTagsStartWith('Sub[lm]', val)
		val = fixAllTagsStartWith('SQL', val)
		val = fixAllTagsStartWith('tfdrfile', val)
		val = fixAllTagsStartWith('TCS ', val)
		val = fixAllTagsStartWith('Tran', val)
		val = fixAllTagsStartWith('timestamp', val)
		val = fixAllTagsStartWith('UF[ET]', val)
		val = fixAllTagsStartWith('Valid', val)
		val = fixAllTagsStartWith('x', val)
		val = fixAllTagsStartWith('Year ', val)
		val = fixAllTagsStartWith('ZFN', val)
		
		// Only save if something changed
		if (steps != val) {
			def retVal = [op:'add', path: '/fields/Microsoft.VSTS.TCM.Steps', value: val]
			return retVal
		}
		return null
	}

	private String fixAllTagsStartWith(String in_word, String in_val) {
		// Address all tags that start with string
		String val = in_val.replaceAll(~"(?i)&lt;[ ]*$in_word[^&]*?&gt;") { it ->
							String s = it
							s = s.replace('&', '&amp;')
							s
						};
		return val
	}
	
	private def genRemoveBadResultLinks(String collection, String project, owi) {
		String url
		int index = 0
	}
	
	private def removeLinkChanges(def wiData, def indexs) {
		
	}
	
	
	
	/**
	 * Refresh work item cache by a query.
	 *
	 * <ul>
	 * <li>Handles deleting duplicates from failure during cache update.</li>
	 * <li>Clears cache prior to update of cache.</li>
	 * <li>Full refresh</li>
	 * </ul>
	 *
	 * @param collection
	 * @param project
	 * @param query
	 * @return
	 */
	def refreshCacheByQuery(String collection, String project, String query, String planId = null, Closure keyC = null) {
		String module = cacheManagementService.cacheModule
		if (trackAdoChanges) {
			// Backup previous cache.
			cacheManagementService.deleteByType(ICacheManagementService.WI_DATA) { key, wi ->
				if (!cacheManagementService.getFromCache(key, 'wiPrevious')) {
					cacheManagementService.saveToCache(wi, key, 'wiPrevious')
				}
			}
		} else {
			cacheManagementService.deleteByType(ICacheManagementService.WI_DATA)
		}
		def wis = getWorkItems(collection, project, query)
		if (!wis || !wis.workItems) return
		def wiList = wis.workItems
		int j = 0
		//def eMap = [:]
		while (true) {
			def ids = []
			def keys = []
			for (int i = 0; i < 200 && j < wiList.size(); i++) {
				def wi = wiList[j]
				ids.push(wi.id)
				j++
			}
			def result = batchGet(collection, project, ids)
			result.value.each { owi ->
				String key = ""
				if (keyC) {
					key = keyC(owi)
				} else {
					key = getKey(owi)
				}
				def cacheWI = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
				if (cacheWI) {
					log.info("Deleting duplicate of (${module}) Element: ${key}, ADO WI: ${owi.id}")
					String url = "${owi.url}"
					if (isTestItem()) {
						deleteTestItem(collection, project, owi)
					} else {
						deleteWorkitem(url, owi)
					}
				} else {
					//eMap[key] = key
					cacheManagementService.saveToCache(owi, key, ICacheManagementService.WI_DATA)
					if (trackAdoChanges) {
						def previous = cacheManagementService.getFromCache(key, 'wiPrevious')
						
						// Remove items from backup that are equivalent to elements being cached.
						// This will be used to determine field changes.
						if (previous) {
							String prev = "${previous.rev}"
							String rev = "${owi.rev}"
							if (prev == rev) {
								cacheManagementService.deleteByIdAndByType(key, 'wiPrevious')
							}
						}
					}
					if (cacheManagementService.cacheModule == 'QM' && planId) {
						refreshTestItemCache(collection, project, key, owi, planId)
					}
				}
			}
			if (j == wiList.size()) {
				def lwi = wiList[j - 1]
				String lastId =  "${lwi.id}"
				j = 0
				wis = getWorkItems(collection, project, query, lastId)
				if (!wis || !wis.workItems || !wis.workItems.size() == 0) return
				wiList = wis.workItems
			}
		}
	}
	
	def refreshTestItemCache(collection, project, String key, def wiCache, String planId) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		if (key.endsWith('Test Suite WI')) {
			String suiteId = "${wiCache.id}"
			def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/testplan/Plans/${planId}/suites/${suiteId}",
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0-preview.1']
				)
			if (result) {
				key = key.substring(0, key.length()-3)
				cacheManagementService.saveToCache(result, key, ICacheManagementService.SUITE_DATA)
			}
		}

	}
	
	boolean isTestItem() {
		String module = cacheManagementService.cacheModule
		return module.equals('SPOCK') || module.equals('TL') || module.equals('QM')
	}
	
	/**
	 * Full refresh of work item cache for a given team area.
	 * <ul>
	 * <li>Handles deleting duplicates from failure during cache update.</li>
	 * <li>Clears cache prior to update of cache.</li>
	 * <li>Full refresh</li>
	 * </ul>
	 *
	 * @param collection - ADO organization
	 * @param project - ADO project
	 * @param teamArea - ADO work item team area.
	 * @return none
	 */
	def refreshCacheByTeamArea(String collection, String project, String teamArea, String planId = null) {
		def moduleMap = ['CCM': 'RTC-', 'RM':'DNG-', 'QM':'RQM-', 'TL': 'TL-']
		String module = cacheManagementService.cacheModule
		String eidPrefix = moduleMap[module]
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [System.AreaPath] under '${teamArea}' AND [Custom.ExternalID] CONTAINS '${eidPrefix}'"
		refreshCacheByQuery(collection, project, query, planId)
	}
	
	private String getKey(def wi) {
		String eId = "${wi.fields['Custom.ExternalID']}"
		if (eId.startsWith('RTC-') || eId.startsWith('DNG-')) {
			return eId.substring(4)
		} else if (eId.startsWith('RQM-')) {
			String wiType = "${wi.fields.'System.WorkItemType'}"
			String key = "${eId.substring(4)}-${wiType}"
			if (wiType != 'Test Case') {
				key = "${key} WI"
			}
			return key
		} else if (eId.startsWith('TL-')) {
			String key = "${eId.substring(3)}"
			return key
		}
	}

	/**
	 * Refreshes cache based upon existing cache elements.
	 * This has a flaw if process is stopped during processing of batch work item cache update.
	 *
	 * <ul>
	 * <li>May look to deprecate this method.</li>
	 * </ul>
	 *
	 * @param collection - ADO organization
	 * @param project - ADO project
	 * @return nothing
	 */
	def refresh(String collection, String project) {
		int i = 0;
		def ids = []
		def keys = []
		def wis = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
		wis.each { key, wi ->
			if (i == 200) {
				def result = batchGet(collection, project, ids)
				int k = 0
				result.value.each { owi ->
					cacheManagementService.saveToCache(owi, keys[k], ICacheManagementService.WI_DATA)
					k++
				}
				ids = []
				keys = []
				i = 0
			}
			ids.push(wi.id)
			keys.push(key)
			i++
		}
		if (ids.size()>0) {
			def result = batchGet(collection, project, ids)
			int k = 0
			result.value.each { owi ->
				cacheManagementService.saveToCache(owi, keys[k], ICacheManagementService.WI_DATA)
				k++
			}
		}
	}
	
	

	private def batchGet(String collection, String project, def ids) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = ['$expand': 'all', ids:ids]
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/workitemsbatch",
				body: body,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0']
				)
		return result

	}

	/**
	 * Clean all work items via this query.
	 *
	 * @param collection - ADO organization
	 * @param project - ADO project
	 * @param query - work item wiql query
	 * @return nothing
	 */
	def clean(String collection, String project, String query) {
		def deleted = [:]
		def wis = getWorkItems(collection, project, query)
		def changelist = []
		while (true) {
			boolean repeatedDelete = false
			wis.workItems.each { wi ->
				if (deleted.containsKey(wi.id)) {
					repeatedDelete = true
					return
				}
				deleteWorkitem(wi.url, wi)
				deleted[wi.id] = wi
			}
			if (repeatedDelete) break
				wis = getWorkItems(collection, project, query)
			if (!wis || wis.workItems.size() == 0) break;
		}
		cacheManagementService.clear()
	}

//	def cleanDuplicates(String collection, String project, int inpage = 0) {
//		def deleted = [:]
//		if (cacheManagementService instanceof MongoDBCacheManagementService) {
//			MongoDBCacheManagementService mdbCacheManagement = cacheManagementService
//			int page = inpage
//			while (true) {
//				def cacheWIs = mdbCacheManagement.getAllOfType('wiData', page)
//				if (cacheWIs.size() == 0) break
//				processCacheDuplicates(collection, project, cacheWIs)
//				page++
//			}
//		} else {
//			def cacheWIs = cacheManagementService.getAllOfType('wiData')
//			processCacheDuplicates( collection, project, cacheWIs )
//		}
//	}
	
	private processCacheDuplicates(String collection, String project, def cacheWIs) {
		cacheWIs.each { key, cacheWI ->
			String eId = "${cacheWI.fields.'Custom.ExternalID'}"
			String cid = "${cacheWI.id}"
			String query = "select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [Custom.ExternalID] = '${eId}'"
			def wis = getWorkItems(collection, project, query)
			if (wis && wis.workItems && wis.workItems.size() > 1) {
				for (int i = 0; i < wis.workItems.size(); i++) {
					def wi = wis.workItems[i]
					String wid = "${wi.id}"
					if (wid != cid) {
						deleteWorkitem(wi.url)
					}
				}
			}
			else if (wis && wis.workItems && "${wis.workItems[0].id}" != "${cid}") {
				String wid = "${wis.workItems[0].id}"
				def wi = getWorkItem(collection, project, wid)
				cacheManagementService.saveToCache(wi, key, 'wiData')
			}
		}

	}

	def getWorkItem(String url) {
		def result = genericRestClient.get(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.3']
			)
		return result
	}

	def getWorkItems(String collection, String project, String aquery, String lastId = null) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query
		if (aquery.contains('ORDER BY'))
			query = [query: "${aquery}"]
		else
			query = [query: "${aquery} ORDER BY [System.Id] ASC"]
		if (lastId != null) {
			query = [query: "${aquery} AND [System.Id] > ${lastId}  ORDER BY [System.Id] ASC"]
		}
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.rateLimitPost(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/wiql",
				body: body,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0-preview.2', '$top': 1000]
				)
		return result

	}

	def getWorkItems(String collection, String aquery) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = [query: aquery]
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/wiql",
				body: body,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0']
				)
		return result

	}
	
	def getQueryWorkItems(String collection, String project, String team, String queryId) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def eteam = URLEncoder.encode(team, 'utf-8')
		eteam = eteam.replace('+', '%20')
		def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/${eteam}/_apis/wit/wiql/${queryId}",
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0']
				)
		return result
	}

	def getWorkItem(String collection, String project, String id) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		//def query = [query: aquery]
		//String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}",
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0', "\$expand": 'All']
				)
		return result

	}
	
	def getWorkItemUpdates(String collection, String project, String id) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		//def query = [query: aquery]
		//String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}/updates",
				//headers: [Accept: 'application/json'],
				query: ['api-version': '6.0', "\$expand": 'All']
				)
		return result

	}

	def getChildren(String collection, String project, String id) {
		def pwi = getWorkItem(collection, project, id)
		//handle deleted children records
		if (!pwi) {
			throw new Exception("$id could not be retrieved.  It may have been deleted")
			return
		}
		
		def childIds = []
		if (!pwi.relations) return childIds // no children
		pwi.relations.each { relation ->
			String rel = "${relation.rel}"
			String url = "${relation.url}"
			if (rel == 'System.LinkTypes.Hierarchy-Forward') {
				int i = url.lastIndexOf('/');
				String cid = null
				if (i != -1) {
					cid = url.substring(i+1);
				}

				childIds.add(cid)
			}
		}
			
		def children = []
		def countrecords = childIds.size()
		if (countrecords > pageSize) {
			def	subChildren = childIds.collate(pageSize)
			subChildren.each{ batch-> 
				def batchChildren = getListedWorkitems(collection, project, batch)
				batchChildren.each{ wi -> children.add(wi)}
				
			}
		} else if (countrecords > 0) {
			
			children = getListedWorkitems(collection, project, childIds)
			
		} 
		
		return children 
	}

	def getParent(String collection, String project, def cwi) {
		//def cwi = getWorkItem(collection, project, id)
		def childIds = []
		def parent = null
		cwi.relations.each { relation ->
			String rel = "${relation.rel}"
			String url = "${relation.url}"
			if (rel == 'System.LinkTypes.Hierarchy-Reverse') {

				parent = getWorkitemViaUrl(url)
				return


			}
		}
		//def children = getListedWorkitems(collection, project, childIds)
		return parent
	}
	

	def getCategories(collection, project) {
		if (categoriesMap.size() == 0) {
			def eproject = URLEncoder.encode(project, 'utf-8')
			eproject = eproject.replace('+', '%20')
			//def query = [query: aquery]
			//String body = new JsonBuilder(query).toPrettyString()
			def result = genericRestClient.get(
					requestContentType: ContentType.JSON,
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitemtypecategories",
					//headers: [Accept: 'application/json'],
					query: ['api-version': '5.0']
					)
			result.value.each { cat ->
				String catName = "${cat.name}"
				cat.workItemTypes.each { wis ->
					String typeName = "${wis.name}"
					categoriesMap[typeName] = catName
				}
			}
		}
		return categoriesMap
	}

	String getCategory(String collection, String project, def wi) {
		String witype = "${wi.fields.'System.WorkItemType'}"
		def catMap = getCategories(collection, project)
		return catMap[witype]
	}

	def deleteWorkitem(String url, def wi) {
		if (wi.fields) {
			String changedBy = "${wi.fields.'System.ChangedBy'.displayName}".toLowerCase()
			if (!changedBy.startsWith('svc-cloud-vs')) return null
		}
		def result = genericRestClient.delete(
				uri: url,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0-preview.3']
				)
		return result

	}
	def deriveOwner(String collection, String project, String closedBy, String id) {
		if (closedBy && closedBy != 'null' && closedBy != '' && closedBy.toLowerCase().indexOf('svc-') == -1)
			return closedBy
		else {
			//Get work item
			def wi = getWorkItem(collection, project, id)
			def wiParent = getParent(collection, project, wi)
			if (wiParent) {
				if (wiParent.fields['System.AssignedTo'] != null)
					return wiParent.fields['System.AssignedTo'].uniqueName
				else if (wiParent.fields['System.State'] == 'Closed') {
					return wiParent.fields['Microsoft.VSTS.Common.ClosedBy'].uniqueName
				}
				else
					return null
			}
			else
				return null
		}
	}

	def deleteTestItem(String collection, String project, def wi) {
		String changedBy = "${wi.fields.'System.ChangedBy'.displayName}".toLowerCase()
		if (!changedBy.startsWith('svc-cloud-vs')) return null
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/test/testcases/${wi.id}"
		def result = genericRestClient.delete(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.1']
			)
		return result
	}

	def refreshCache(def collection, def project, def cacheIds) {
		def vstsIds = []
		def idMap = [:]
		int count = 0
		cacheIds.each { id ->
			def wi = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
			if (wi != null) {
				String vstsId = "${wi.id}"
				vstsIds.add(vstsId)
				idMap[count] = id
				count++
			}
		}
		def vstsWIs = getListedWorkitems(collection, project, vstsIds)
		count = 0
		vstsWIs.each { wi ->
			cacheManagementService.saveToCache(wi, idMap[count], ICacheManagementService.WI_DATA)
			count++
		}
	}

	def getWorkitemViaUrl(String url) {
		def result = genericRestClient.get(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: url,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.0']
				)
		return result
	}

	def getListedWorkitems(def collection, def project, def vstsIds) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		//def projectData = projectManagementService.getProject(collection, project)

		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems",
				headers: [Accept: 'application/json'],
				query: [ids: vstsIds.join(','), 'api-version': '4.1', '\$expand': 'all' ]
				)
		if (result == null) {
			return []
		}
		return result.value
	}

	/**
	 * Submit batch of work item changes to TFS/VSTS.
	 * @param collection
	 * @param project
	 * @param witData - batch of work item changes
	 * @return
	 */
	def batchWIChanges(def collection, def changeList, def idMap) {
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		int bcount = 0
		def bidMap = [:]
		def bchangeList = []
		int tcount = 0
		int count = changeList.size()
		while (tcount < count) {
			bidMap[bcount] = idMap[tcount]
			bchangeList.add(changeList[tcount])
			bcount++
			if (bcount == BATCHSIZE) {
				doPost(collection, bchangeList, bidMap)
				bcount = 0
				bidMap = [:]
				bchangeList = []
			}
			tcount++
		}
		if (bcount > 0) {
			doPost(collection, bchangeList, bidMap)

		}
	}

	private doPost(collection, bchangeList, bidMap) {
		def body = new JsonBuilder(bchangeList).toPrettyString()
		def result = genericRestClient.rateLimitPost(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/\$batch",
				body: body,
				headers: [accept: 'application/json'],
				query: ['api-version': '4.1']

				)
		if (result != null) {
			cacheResult(result, bidMap)
		} else {
			if (checkpointManagementService != null) {
				checkpointManagementService.addLogentry("Batch request failed!")
			}
			log.error("Batch request failed!")
		}

	}
	
	public def createWorkItem(collection, project, type, data, Closure responseHandler = null) {
		def etype = URLEncoder.encode("$type", 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def body = new JsonBuilder(data).toPrettyString()
		def result = genericRestClient.post(
					[contentType: 'application/json',
					requestContentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/\$${etype}",
					body: body,
					query: ['api-version': '5.0', bypassRules:true],
					headers: ['Content-Type': 'application/json-patch+json']]
					)
		return result
	}
	public addWorkItemComment(collection, project, id, data) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def body = new JsonBuilder(data).toPrettyString()
		def result = genericRestClient.post(
					[contentType: 'application/json',
					requestContentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}/comments",
					body: body,
					query: ['api-version': '5.1-preview.3'],
					headers: ['Content-Type': 'application/json']]
					)
	}
	public def updateWorkItem(collection, project, id, data, Closure responseHandler = null) {
			def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
			//def body = new JsonBuilder(data).toPrettyString()
			def result = genericRestClient.patch(
						[contentType: 'application/json',
						requestContentType: 'application/json',
						uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/workitems/${id}",
						body: data,
						query: ['api-version': '5.0', bypassRules:true],
						headers: ['Content-Type': 'application/json-patch+json']]
						,responseHandler
						)
						
			return result
	}
	
	def cacheResult(result, idMap) {
		int count = 0
		int mapSize = idMap.size()
		if (!result.value) return
		int retCount = result.value.size()
		if (retCount > 0 && retCount < mapSize) {

			result.value.each { resp ->
				if (count == 0) {
					def issue = new JsonSlurper().parseText(resp.body)
					log.error "Failed to save full batch of work items:  WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}"
					if (checkpointManagementService != null) {
						checkpointManagementService.addLogentry("Failed to save full batch of work items, Error:  ${issue.'value'.Message}")
					}
				}
				else if (count > 1) {
					if ("${resp.code}" == '200') {
						def wi = new JsonSlurper().parseText(resp.body)
						String id = idMap[count]
						if (cacheManagementService != null) {
							cacheManagementService.saveToCache(wi, idMap[count], ICacheManagementService.WI_DATA)
						}
					} else {
						def issue = new JsonSlurper().parseText(resp.body)
						log.error("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
						if (checkpointManagementService != null) {
							checkpointManagementService.addLogentry("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
						}
					}
				}
				count++
			}
		} else {
			result.value.each { resp ->
				if ("${resp.code}" == '200') {
					def wi = new JsonSlurper().parseText(resp.body)
					String id = idMap[count]
					if (cacheManagementService != null) {
						cacheManagementService.saveToCache(wi, idMap[count], ICacheManagementService.WI_DATA)
					}
				} else {
					def issue = new JsonSlurper().parseText(resp.body)
					log.error("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
					if (errorCaching && cacheManagementService != null) {
						def wiCache = cacheManagementService.getFromCache(idMap[count], ICacheManagementService.WI_DATA)
						cacheManagementService.saveToCache([item:wiCache, error: issue.'value'.Message] , idMap[count], 'wiErrored')
					}
					if (checkpointManagementService != null) {
						checkpointManagementService.addLogentry("WI:  ${idMap[count]} failed to save, Error:  ${issue.'value'.Message}")
					}
				}
				count++
			}
		}
	}





}
