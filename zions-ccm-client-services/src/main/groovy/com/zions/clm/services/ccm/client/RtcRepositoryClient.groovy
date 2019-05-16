package com.zions.clm.services.ccm.client

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * @author z091182
 *
 * Client entry point for CLM tooling.
 * o provides login
 * o provides api service that accesses other service api interfaces.
 */
@Component
public class RtcRepositoryClient implements Serializable {

	transient String uri;
	transient String username;
	transient String password;
	transient ITeamRepository repo;
	transient IProgressMonitor monitor;
	
	@Autowired
	public RtcRepositoryClient(@Value('${clm.url}') String uri, @Value('${clm.user}') String username, @Value('${clm.password}') String password) {
		this.uri = uri;
		this.username = username;
		this.password = password;
		monitor = new NullProgressMonitor();
		initializeRTC()
	}
	
	public void initializeRTC() throws TeamRepositoryException {
		if (!TeamPlatform.isStarted()) {
			TeamPlatform.startup();
		}
		repo = TeamPlatform.getTeamRepositoryService().getUnmanagedRepository("${uri}/ccm");
		performLogin();
	}
	
	/**
	 *  Login 
	 * @throws TeamRepositoryException
	 */
	private void performLogin() throws TeamRepositoryException {
		if (repo.loggedIn()) {
			repo.logout();
		}
		def handler = new RTCLoginHandler()
		handler.userId = username
		handler.password = password
		repo.registerLoginHandler(handler);
		repo.login(null);
	}
	
	/**
	 * Entry point to access all other service interfaces.
	 * 
	 * @return
	 */
	public ITeamRepository getTeamRepository() {
		return repo;
	}
	
	public IProgressMonitor getProgressMonitor() {
		return monitor;
	}
	
	public void shutdownPlatform(){
		TeamPlatform.shutdown();
	}
}
