package com.zions.clm.services.ccm.workitem.metadata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.ibm.team.links.common.internal.ReferenceUtils
import com.ibm.team.process.client.IProcessItemService
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.common.IFetchResult
import com.ibm.team.repository.common.TeamRepositoryException
import com.ibm.team.workitem.api.common.IWorkItem
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.AttributeTypes
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.IAttributeHandle
import com.ibm.team.workitem.common.model.IEnumeration
import com.ibm.team.workitem.common.model.ILiteral
import com.ibm.team.workitem.common.model.IWorkItemType
import com.ibm.team.workitem.common.model.WorkItemLinkTypes
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.utils.ReferenceUtil
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

@Component
@Slf4j
class CcmWIMetadataManagementService {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
		
	def jsonMetaData = [:]

	public CcmWIMetadataManagementService() {
		
	}
	
	private IProjectArea findProjectArea(def projectArea) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IProcessItemService  service = (IProcessItemService) teamRepository.getClientLibrary(IProcessItemService.class);
		def pAreas = service.findAllProjectAreas(null, null)
		IProjectArea retVal = null
		pAreas.each { IProjectArea pArea ->
			if (pArea.getName() == "${projectArea}") {
				retVal = pArea
			}
		}
		return retVal
	}
	
	def extractWorkitemMetadataJson(IProjectArea pArea) {
		if (jsonMetaData.size() != 0) return jsonMetaData
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		List<IWorkItemType> wits = common.findWorkItemTypes(pArea, null)
		
		
		wits.each { IWorkItemType wit ->
			
			String witId = "${wit.identifier}"
			jsonMetaData[witId.toLowerCase()] = getAttributes(wit, pArea)
		}
		return jsonMetaData
	}

	def getAttributes(IWorkItemType wit, pArea) {
		def attrs = [:]
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
        List<IAttributeHandle> builtInAttributeHandles = workItemClient 
                .findBuiltInAttributes(pArea, null); 
        IFetchResult builtIn = teamRepository.itemManager() 
                .fetchCompleteItemsPermissionAware(builtInAttributeHandles, 
                        IItemManager.REFRESH, null); 

		List<IAttribute> bAttrs = builtIn.getRetrievedItems()
		bAttrs.each { IAttribute attr ->
			def attrData = [id: attr.identifier, displayName: attr.displayName]
			attrs[attr.displayName] = attrData
		}
        List<IAttributeHandle> custAttributeHandles = wit 
                .getCustomAttributes();
         
        IFetchResult custom = teamRepository.itemManager() 
                .fetchCompleteItemsPermissionAware(custAttributeHandles, 
                        IItemManager.REFRESH, null); 
		List<IAttribute> cAttrs = custom.getRetrievedItems()
		cAttrs.each { IAttribute attr ->
			def attrData = [id: attr.identifier, displayName: attr.displayName]
			attrs[attr.displayName] = attrData
		}
		
		ReferenceUtil.ALL_LINK_TYPES.each { link ->
			def attrData = [id: link, displayName: link]
			attrs[link] = link
		}
		return attrs
	}
	
	def extractWorkitemMetadata(def projectArea, def templateDir) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IProjectArea pArea = findProjectArea(projectArea)
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		List<IWorkItemType> wits = common.findWorkItemTypes(pArea, null)
		
		wits.each { wit ->
			def xml = generateMetadata(wit, pArea)
			File oFile = new File("${templateDir}/${wit.displayName}.xml");
			def w = oFile.newWriter();
			w << "${xml}"
			w.close();
		}
	}
	
	def generateMetadata(IWorkItemType wit, IProjectArea pArea) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
        List<IAttributeHandle> builtInAttributeHandles = workItemClient 
                .findBuiltInAttributes(pArea, null); 
        IFetchResult builtIn = teamRepository.itemManager() 
                .fetchCompleteItemsPermissionAware(builtInAttributeHandles, 
                        IItemManager.REFRESH, null); 

		List<IAttribute> bAttrs = builtIn.getRetrievedItems()
		
        List<IAttributeHandle> custAttributeHandles = wit 
                .getCustomAttributes();
         
        IFetchResult custom = teamRepository.itemManager() 
                .fetchCompleteItemsPermissionAware(custAttributeHandles, 
                        IItemManager.REFRESH, null); 
		List<IAttribute> cAttrs = custom.getRetrievedItems()
		
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)
		bXml.'witd:WITD'(application:'Work item type editor',
			version: '1.0',
			'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
			WORKITEMTYPE(name: "${wit.displayName}") {
				DESCRIPTION('general work item starter')
				FIELDS {
					bAttrs.each { attr ->
						FIELD(name: "${attr.displayName}", refname: "${attr.identifier}", type: "${attr.attributeType}".trim(), dimension: 'reportable') {
							HELPTEXT "From rtc '${attr.displayName}'"
							AttributeTypes t
							if (AttributeTypes.isEnumerationListAttributeType(attr.identifier) || AttributeTypes.isEnumerationAttributeType(attr.attributeType)) {
								
								ALLOWEDVALUES(expanditems: true) {
									def values = getValuesForIdentifier(attr)
									values.each { value ->
										LISTITEM(value: value)
									}
								}
							}
						}
					}
					cAttrs.each { attr ->
						FIELD(name: "${attr.displayName}", refname: "${attr.identifier}", type: "${attr.attributeType}".trim(), dimension: 'reportable') {
							HELPTEXT "From rtc '${attr.displayName}'"
							if (AttributeTypes.isEnumerationListAttributeType(attr.attributeType) || AttributeTypes.isEnumerationAttributeType(attr.attributeType)) {
								ALLOWEDVALUES(expanditems: true) {
									def values = getValuesForIdentifier(attr)
									values.each { value ->
										LISTITEM(value: value)
									}
								}
							}
						}
					}

				}
			}
		}
		return writer.toString()
		
	}
	
	def getValuesForIdentifier(IAttributeHandle ia) throws TeamRepositoryException {
		def retVal = []
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemCommon workItemCommon = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		IEnumeration<? extends ILiteral> enumeration = workItemCommon.resolveEnumeration(ia, null);
		List<? extends ILiteral> literals = enumeration.getEnumerationLiterals();
		for (Iterator<? extends ILiteral> iterator = literals.iterator(); iterator.hasNext();) {
			ILiteral iLiteral = (ILiteral) iterator.next();
			retVal.add(iLiteral.getName())
		}
		return retVal;

	}

}
