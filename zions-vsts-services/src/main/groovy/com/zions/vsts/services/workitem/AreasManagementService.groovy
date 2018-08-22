package com.zions.vsts.services.workitem

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
class AreasManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService

	public AreasManagementService() {
		
	}
	
	def processAreasData(def collection, def project, def areas, def parent, def currentAreas) {
		areas.each { area -> 
			
			ensureArea(collection, project, area, parent, currentAreas)
			processAreasData(collection, project, area.children, area, currentAreas)
		}
	}
	
	def getRelated(area, currentAreas) {
		if (area == null) {
			return currentAreas.treeValues[0]
		}
		def related = null
		String[] areaLoc = 	"${area.id}".split('/')
		def cAreas = currentAreas.treeValues[0]
		areaLoc.each { node ->
			//boolean flag = false
			
			def found = null
			cAreas.children.each { child ->
				if ("${node}" == "${child.text}") {
					found = child
				}
			}
			if (found != null) {
				cAreas = found
			}
			related = found
		}
		return related
	}
	
	
	def ensureArea(def collection, def project, def area, def parent, def currentAreas) {
		def relatedArea = getRelated(area, currentAreas)
		if (relatedArea == null) {
			def relatedParent = getRelated(parent, currentAreas)
			createArea(collection, project, area, relatedParent)
		}
		
	}
	
	def getAreaData(def collection, def project) {
		def eproject = URLEncoder.encode(project, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.HTML,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_admin/_Work",
			//headers: headers,
			query: ['_a':'areas']
			)
		def dataNode = result.'**'.find { node ->
			if ("${node.name()}".toLowerCase() == 'div' && "${node.@class}".toLowerCase() == 'project-admin-work') {
				return true
			}
			return false
		}
		def areas = null
		if (dataNode != null) {
			def json = dataNode.SCRIPT.text()
			JsonSlurper s = new JsonSlurper()
			def config = s.parseText(json)
			areas = config.areas
		}
		return areas
	}
	
	def createArea(def collection, def project, def area, def parent) {
		def operationData = [ 'NodeId': '00000000-0000-0000-0000-000000000000',  'NodeName': "${area.name}", 'IterationStartDate':null, 'IterationEndDate': null, 'ParentId': "${parent.id}" ]
		def opData = new JsonBuilder( operationData ).toString()
		def bodyData = ['operationData': opData, syncWorkItemTracking: false]
		def body = new JsonBuilder( bodyData ).toString()
		def eproject = URLEncoder.encode(project.name, 'UTF-8')
		eproject = eproject.replace('+', '%20')

		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_admin/_Areas/CreateClassificationNode",
			query: ['__v': 5, useApiUrl: true],
			body: body,
			headers: [Accept: 'application/json, text/javascript, */*; q=0.01',
				Referer: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_admin/_Work?_a=areas"],
			)
		return result

	}
	
	def assignTeamAreas(def collection, def project, def teams) {
		teams.each { team, areas ->
			assignToTeam(collection, project, team, areas)
		}
	}
	
	def assignToTeam(collection, project, team, areas) {
		def teamData = projectManagementService.getTeam(collection, project.name, team)
		if (teamData == null) return
		def outAreas = []
		areas.each { area ->
			String value = "${area.id}".replace('/', '\\')
			value = "${project.name}\\${value}"
			def data = ['value': value]
			outAreas.add(data)
		}
		def saveData = ['DefaultValueIndex': 0, 'TeamFieldValues': outAreas]
		def saveDataS = new JsonBuilder( saveData ).toString()
		def bodyData = ['saveData': saveDataS]
		def body = new JsonBuilder( bodyData ).toString()
		def eproject = URLEncoder.encode(project.name, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def eteam = URLEncoder.encode(teamData.name, 'UTF-8')
		eteam = eproject.replace('+', '%20')
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/${teamData.id}/_admin/_Areas/UpdateAreasData",
			query: ['__v': 5, useApiUrl: true],
			body: body,
			headers: [Accept: 'application/json, text/javascript, */*; q=0.01',
				Referer: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/${eteam}/_admin/_Work?_a=areas"],
			)

	}
}
