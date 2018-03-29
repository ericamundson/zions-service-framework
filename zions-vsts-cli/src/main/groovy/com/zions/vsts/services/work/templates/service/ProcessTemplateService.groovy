package com.zions.vsts.services.work.templates.service
import com.zions.vsts.services.rest.GenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service;

@Service
class ProcessTemplateService {
	
	@Autowired
	private GenericRestClient restClient;
	
	
	
    public ProcessTemplateService() {
		
	}
}
