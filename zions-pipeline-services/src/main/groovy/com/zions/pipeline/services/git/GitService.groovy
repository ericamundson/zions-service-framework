package com.zions.pipeline.services.git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.eclipse.jgit.api.Git
import java.net.ProxySelector
import java.net.Proxy
import java.net.Proxy.Type
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

@Component
class GitService {
	
	@Value('${tfs.token:}')
	String tfsToken
	
	@Value('${repo.location:}')
	File repos
	
	
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
	
	File loadChanges(String url,String repoName) {
		String nUrl = url
		if (nUrl.contains('https://dev')) {
			nUrl = nUrl.replace('https://dev', "https://${tfsToken}@dev")
			
		}
		File repo = new File(repos, "${repoName}")
		if (!repo.exists()) {
			repo.mkdirs()
		}
		File dotGit = new File(repo, '.git')
		if (!dotGit.exists()) {
			Git.cloneRepository()
				.setURI(nUrl)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))
				.setDirectory(repo)
				.call();
		} else {
			Git.open(repo)
				.pull()
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider('',"${tfsToken}"))
				.call()
		}
			
		return repo
	}
}