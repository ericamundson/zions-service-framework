package com.zions.pipeline.services.yaml.template.execution
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService

/**
 * Accepts yaml in the form (some items will vary by endpointType and authorization scheme):
 * executables:
 * - type: serviceConnection
 *   context: ZionsETO
 *   projects: ReleaseEngineering
 *   endpointType: xldserver
 *   endpointUrl: https://xldeploy.cs.zionsbank.com
 *   endpointName: XL Deploy - Prod
 *   endpointDescription: XL Deploy for production environment
 *   grantAllPerm: true
 *	 authorization:
 *	   scheme: UsernamePassword
 *	   username: ${svc-xld.user}
 *	   password: ${svc-xld.password}
 *
 *   vault:
 *     engine: secret
 *     path:  zions-service-framework/svc-connection

 * @author z091556
 *
 */
@Component
class ServiceConnection implements IExecutableYamlHandler {
	

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService

	def handleYaml(def yaml, File containedRepo, def locations, String branch, String projectName) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		String endpointType = yaml.endpointType
		def epAuthScheme = [ scheme: "UsernamePassword", parameters: [username: yaml.authorization.username, password: yaml.authorization.password] ]
		def epData = [ acceptUntrustedCerts: false ]
		def endpointData = [ administratorsGroup: null, authorization: epAuthScheme, createdBy: null, data: epData, description: yaml.endpointDescription, groupScopeId: null,	name: yaml.endpointName, operationStatus: null, readersGroup: null, serviceEndpointProjectReferences: [],type: yaml.endpointType, url: yaml.endpointUrl, isShared: false, owner: "library" ]
 
		def epProjectReferences = []
		String projects = "${yaml.projects}"
		if ( projects == 'all') {
			def allProjects = projectManagementService.getProjects('')
			allProjects.'value'.each { project ->
				epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointDescription, projectReference: [ id: project.id, name: project.name ] ] ]
				endpointData.serviceEndpointProjectReferences = epProjectReferences
				endpointManagementService.ensureServiceEndpoint(project, endpointData, yaml.grantAllPerm)
			}
		} else {
			String[] projectList = projects.split(',')
			projectList.each { String pName ->
				//System.out.println("In handleYaml - Calling projectManagementService.getProject for: " + pName)
				def project = projectManagementService.getProject('', pName)
				if (project) {
					epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointDescription, projectReference: [ id: project.id, name: project.name ] ] ]
					endpointData.serviceEndpointProjectReferences = epProjectReferences
					endpointManagementService.ensureServiceEndpoint(project, endpointData, yaml.grantAllPerm)
				}
			}
		}
	}
}
