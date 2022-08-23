package com.zions.vsts.services.work.templates

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j


@Component
@Slf4j
class BacklogHierarchyService {
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Value('${tfs.collection:}')
	String collection

	String project = 'BaNCS' // Default project to retrieve process config
	
	def static categories = []
	def static witCategoryMap = [:]
	def static categoryLevelMap = [:]
	
	// Get all wits mapped to associated category name
	public def getWitCategoryMap() {
		if (witCategoryMap.size() > 0) return witCategoryMap
		
		buildProcessCategories()

		categories.each { cat ->
			cat.wiTypes.each { type ->
				witCategoryMap.put("$type".toString(), "${cat.name}".toString())
			}
		}
		return witCategoryMap
	}
	
	// Get all backlog categories with associated numeric level #
	public def getCategoryLevelMap() {
		if (categoryLevelMap.size() > 0) return categoryLevelMap
		
		buildProcessCategories()
		
		int level = 1
		categories.each { cat ->
			categoryLevelMap.put("${cat.name}".toString(), level++)
		}
		return categoryLevelMap
	}
	
	private def buildProcessCategories() {
		if (categories.size() > 0) return
		
		// Retrieve process config for this process template
		def processConfig = processTemplateService.getProcessConfiguration(collection, project)
		if (!processConfig) {
			log.error("Unable to retrieve backlog configuration for $collection")
			return
		}
		def portfolioBacklogs = processConfig.portfolioBacklogs
		def requirementBacklog = processConfig.requirementBacklog
		def taskBacklog = processConfig.taskBacklog
		
		// Construct categories list
		// The first 2 levels of portfolioBacklog are not returned by API - so construct manually
		categories.add(new Category('LevelOne', ['LevelOne']))
		categories.add(new Category('PortfolioEpic', ['Portfolio Epic']))
		
		// Next add portfolioBacklogs categories retrieved from ADO
		portfolioBacklogs.each { backlog ->
			addCategory(backlog)
		}
		
		// Next add requirementBacklog category retrieved from ADO
		Category reqCat = addCategory(requirementBacklog)
		// Add Bug since it is not defined in ADO Backlog but still considered to be requirement
		reqCat.addWit('Bug')
		
		// Next add taskBacklog category retrieved from ADO
		addCategory(taskBacklog)
	}
	
	private def addCategory(def backlog) {
		def workItemTypes = []
		backlog.workItemTypes.each { type ->
			workItemTypes.add("${type.name}".toString())
		}
		def newCat = new Category("${backlog.name}".toString(), workItemTypes)
		categories.add(newCat)
		return newCat
	}

	public class Category
	{
		String name
		def wiTypes = []
		
		def Category(String name, def wiTypes) {
			this.name = name
			this.wiTypes = wiTypes
		}
		
		def addWit(witName) {
			wiTypes.add(witName)
		}
	}
}
