package com.zions.pipeline.services.git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.lib.StoredConfig
//import org.eclipse.jgit.api.CloneResult
import java.net.ProxySelector
import java.net.Proxy
import java.net.Proxy.Type
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import com.zions.vsts.services.code.CodeManagementService

@Component
class GitService {
	
	@Value('${tfs.token:}')
	String tfsToken
	
	@Value('${win.repo.location:}')
	String winRepoLocation
	
	@Value('${unix.repo.location:}')
	String unixRepoLocation

	File repos
	
	@Autowired
	CodeManagementService codeManagementService
	
	
	public GitService() {
		init()
	}
	
	def init() {
		Authenticator.setDefault(new Authenticator() {
		    @Override
		    protected PasswordAuthentication getPasswordAuthentication() {
		        if (getRequestorType() == RequestorType.PROXY) {
		            String prot = getRequestingProtocol().toLowerCase();
		            String host = System.getProperty(prot + ".proxyHost", "");
		            String port = System.getProperty(prot + ".proxyPort", "8080");
		            String user = System.getProperty(prot + ".proxyUser", "");
		            String password = System.getProperty(prot + ".proxyPassword", "");
		
		            if (getRequestingHost().equalsIgnoreCase('172.18.4.115')) {
		                if (Integer.parseInt(port) == getRequestingPort()) {
		                    // Seems to be OK.
		                    return new PasswordAuthentication(user, password.toCharArray());  
		                }
		            }
		        }
		        return null;
		    }  
		});	
		ProxySelector.setDefault(new ProxySelector() {
			final ProxySelector delegate = ProxySelector.getDefault();
		
			@Override
			public List<Proxy> select(URI uri) {
					// Filter the URIs to be proxied
				if (uri.toString().contains("dev.azure")
						&& uri.toString().contains("https")) {
					return Arrays.asList(new Proxy(Type.HTTP, InetSocketAddress
							.createUnresolved("172.18.4.115", 8080)));
				}
					// revert to the default behaviour
				return delegate == null ? Arrays.asList(Proxy.NO_PROXY)
						: delegate.select(uri);
			}
		
			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				if (uri == null || sa == null || ioe == null) {
					throw new IllegalArgumentException(
							"Arguments can't be null.");
				}
			}
		});
	}
	
	String getProjectName(String url) {
		String[] uSplit = url.split('/')
		String outStr = null
		try {
			outStr = uSplit[4]
		} catch (e) {}
		return outStr
	}
	
	File loadChanges(String url,String repoName = null, String branch = null, boolean reload = false, String userName = null) {
		if (!repos) {
			String osname = System.getProperty('os.name')
			
			if (osname.contains('Windows')) {
				repos = new File(winRepoLocation)
			} else {
				repos = new File(unixRepoLocation)
			}

		}
		if (!repoName) {
			int nIndex = url.lastIndexOf('/')+1
			repoName = url.substring(nIndex)
		}
		String projectName = getProjectName(url)
		
		if (!branch) {
			def adoRepo = codeManagementService.getRepoWithProjectName('', projectName, repoName)
			branch = adoRepo.defaultBranch
		}
		String aBranch = branch
		if (aBranch.startsWith('refs/heads/')) {
			aBranch = aBranch.substring( 'refs/heads/'.length())
		}
		String nUrl = url
		if (nUrl.contains('dev.')) {
			String subStr = nUrl.substring(0, nUrl.indexOf('.'))
			nUrl = nUrl.replace(subStr, "https://${tfsToken}@dev")
			
		}
		File projectDir = new File(repos, "${projectName}")
		File repo = new File(projectDir, "${repoName}")
		if (reload && repo.exists()) {
			boolean success = repo.deleteDir()
			if (!success) {
				return null
			}
		}
		if (!repo.exists()) {
			repo.mkdirs()
		}
		File dotGit = new File(repo, '.git')
		if (!dotGit.exists()) {
			Git git = null
			try {
				git = Git.cloneRepository()
				.setURI(nUrl)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))
				.setDirectory(repo)
				.setBranchesToClone(Arrays.asList(branch))
				.setBranch(branch)
				.setRemote('origin')
				.call();
				setUser(git, userName)
			} finally {
				if (git) {
					git.close()
				}
			}
		} else {
			File lockFile = new File(dotGit, 'index.lock')
			int i = 0;
			while (lockFile.exists()) {
				File irepo = new File(repos, "${repoName}${i}")
				File irepoDGit = new File(repos, "${repoName}${i}/.git")
				lockFile = new File(repos, "${repoName}${i}/.git/index.lock")
				if (!lockFile.exists()) {
					if (!irepoDGit.exists()) {
						irepo.mkdirs()
						Git git = null
						try {
							git = Git.cloneRepository()
							.setURI(nUrl)
							.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))
							.setDirectory(irepo)
							.setBranchesToClone(Arrays.asList(branch))
							.setBranch(branch)
							.setRemote('origin')
							.call();
							setUser(git, userName)
						} finally {
							if (git) {
								git.close()
							}
						}
						
						//return irepo
					} else {
						Git git = null
						try {
							Git.open(irepo)
							git.reset()
							.setMode(ResetType.HARD)
							.call()
							StoredConfig config = git.getRepository().getConfig();
							config.setString("remote", "origin", "url", nUrl);
							config.save();
							PullCommand pc = git.pull()
							pc.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))			
							.configure(pc)
							.setRemote("origin")
							.setRemoteBranchName(aBranch)
							.call()
							setUser(git, userName)
						} finally {
							if (git) {
								git.close()
							}
						}
						//return irepo
					}
					return irepo
				}
			}
			Git git = null
			try {
				git = Git.open(repo)
				git.reset()
				.setMode(ResetType.HARD)
				.call()
				StoredConfig config = git.getRepository().getConfig();
				config.setString("remote", "origin", "url", nUrl);
				config.save();
							
				PullCommand pc = git.pull()
				pc.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))			
				.setRemote("origin")
				.setRemoteBranchName(aBranch)
				.call()
				setUser(git, userName)
			} finally {
				if (git) {
					git.close()
				}
			}
		}
			
		return repo
	}
	
	def setUser(Git git, String userName) {
		if (!userName || userName.length() == 0) return
		StoredConfig config = git.getRepository().getConfig();
		config.setString("user", null, "name", userName);
		config.save();
	}
	
	def checkout(File repo, String branchName, boolean create = false) {
		if (branchName.startsWith('refs/heads/')) {
			branchName = branchName.substring( 'refs/heads/'.length())
		}
		Git git = Git.open(repo)
		git.checkout()
		.setCreateBranch(create)
		.setName(branchName)
		.call()
		git.close()
	}
	
	def deleteBranch(File repo, String branchName) {
		if (branchName.startsWith('refs/heads/')) {
			branchName = branchName.substring( 'refs/heads/'.length())
		}
		Git git = Git.open(repo)
		String[] branches = [branchName]
		git.branchDelete()
		.setBranchNames(branches)
		.setForce(true)
		.call()
		git.close()
	}

	def reset(File repo) {
		Git git = Git.open(repo)
		git.reset()
		.setMode(ResetType.HARD)
		.call()
		git.close()
		
	}
	
	def open(File repo) {
		Git git = Git.open(repo)
		return git
	}
	
	def close(Git git) {
		git.close();
	}

	def pushChanges(File repo, String filePattern = '.', String comment = 'Update pipeline') {
		Git git = Git.open(repo)
		git.add().addFilepattern(filePattern).call();
		git.add().setUpdate(true).addFilepattern(filePattern).call();
		git.commit().setMessage(comment).call();
		
		git.push()
		  .setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))
		  .call()
		git.close()
	}
	
	String getBranchName(File repo) {
		Git git = Git.open(repo)
		org.eclipse.jgit.lib.Repository r = git.getRepository()
		
		return r.fullBranch
	}
}
