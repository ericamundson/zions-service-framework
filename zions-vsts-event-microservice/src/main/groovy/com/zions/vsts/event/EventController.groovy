package com.zions.vsts.event;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import javax.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.client.RestTemplate
import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;

/**
 * ReST Controller for forwarding Azure DevOps events.
 * @author James McNabb
 * 
 */
@RestController
@Slf4j
public class EventController {

	private Map services = new HashMap<String,List>()

	//@Autowired
	//private HttpServletRequest request

    /**
     * Parse event type from request body and forward the request to the appropriate service. 
     *  
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity forwardADOEvent(@RequestBody String body, HttpMethod method, HttpServletRequest request) throws URISyntaxException {
		log.debug("EventController::forwardADOEvent - Request body:\n"+body)
		def eventData = new JsonSlurper().parseText(body)
		// handle multiple service mappings for a single event type
		//def serviceDetails = getServiceDetails("${eventData.eventType}")
		String eventType = "${eventData.eventType}"
		List svcMappings = this.services.get(eventType)
		if (svcMappings == null) {
			// Did not find a valid service mapping for which to forward the request
			def errMessage = "No service mappings for event type: ${eventData.eventType}. Unable to forward the request."
			log.error("${errMessage}")
			return new ResponseEntity<String>("${errMessage}", HttpStatus.UNPROCESSABLE_ENTITY)
		}
		log.debug("Found service mappings for event ${eventData.eventType}")
		for (java.util.ListIterator<RegisteredService> iter = svcMappings.listIterator(); iter.hasNext(); ) {
			RegisteredService regSvc = iter.next()
			String svcName = regSvc.serviceName
			log.debug("Mapping is for " + svcName + " service")
			// TODO: if there are multiple mappings for same event type / serviceName, round robin the requests
			log.debug("Create URI for target service: "+request.getRequestURI())
			URI uri = new URI("${regSvc.protocol}", null, "${regSvc.serverUrl}", regSvc.port, request.getRequestURI(), request.getQueryString(), null)
			RestTemplate restTemplate = new RestTemplate()
			log.debug("Forward request to target service ...")
			ResponseEntity<String> respEntity = restTemplate.exchange(uri, method, new HttpEntity<String>(body, createHeaders(request)), String.class)
			return respEntity
			//return ResponseEntity.ok(HttpStatus.OK)
		}
	}

    /**
     * Register a service as a handler for a given event type. 
     *  
     * @return
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
	public ResponseEntity registerServiceForEvent(@RequestBody String body) {
		log.debug("EventController::registerServiceForEvent - Request body:\n"+body)
		def serviceData = new JsonSlurper().parseText(body)
		RegisteredService svc = new RegisteredService()
		svc.serviceName = "${serviceData.serviceName}"
		svc.serverUrl = "${serviceData.serverUrl}"
		svc.port = new Integer("${serviceData.port}").intValue()
		svc.protocol = "${serviceData.protocol}"
		String svcID = registerServiceMapping(svc, "${serviceData.eventType}")
		return new ResponseEntity<Object>(["svcUUID":svcID], HttpStatus.OK)
	}

    /**
     * Register a service as a handler for a given event type. 
     *  
     * @return
     */
    @RequestMapping(value = "/unregister", method = RequestMethod.POST)
	public ResponseEntity unregisterServiceForEvent(@RequestBody String body) {
		log.debug("EventController::unregisterServiceForEvent - Request body:\n"+body)
		def serviceData = new JsonSlurper().parseText(body)
		String status = unregisterServiceMapping("${serviceData.svcUUID}", "${serviceData.eventType}")
		if (status != "ok") {
			return new ResponseEntity<String>(status, HttpStatus.NOT_FOUND)
		}
		return new ResponseEntity<String>("OK", HttpStatus.OK)
	}

    private def getServiceDetails(String eventType) {
		log.debug("In getServiceDetails - eventType = "+eventType)
		List svcMappings = this.services.get(eventType)
		if (svcMappings == null) {
			return null
		}
		RegisteredService registeredSvc = null
		for (java.util.ListIterator<RegisteredService> iter = svcMappings.listIterator(); iter.hasNext(); ) {
			registeredSvc = iter.next()
			String svcName = registeredSvc.serviceName
			// TODO: if there are multiple mappings for same event type / serviceName, round robin the requests
			
		}
    	return registeredSvc
    }

    private HttpHeaders createHeaders(HttpServletRequest request) {
    	HttpHeaders requestHeaders = new HttpHeaders();
		log.debug("Getting headers from event request ...")
		java.util.Enumeration hdrNames = request.getHeaderNames()
		while (hdrNames.hasMoreElements()) {
			String hdrName = hdrNames.nextElement()
			java.util.Enumeration theHdrs = request.getHeaders(hdrName)
			while (theHdrs.hasMoreElements()) {
				String hdrValue = theHdrs.nextElement()
				log.debug("Setting " + hdrName + " header to: " + hdrValue)
				requestHeaders.set(hdrName,hdrValue);
			}
		}
		//log.debug("Returning Headers ...")
		return requestHeaders
    }

	private String registerServiceMapping(RegisteredService svc, String eventType) {
		log.debug("In registerServiceMapping - eventType = "+eventType)
		// generate a unique for the service mapping
		svc.id = generateUUID()
		java.util.List svcMappings = this.services.get(eventType)
		if (svcMappings == null) {
			svcMappings = new java.util.ArrayList()
		}
		svcMappings.add(svc)
		this.services.put(eventType, svcMappings)
		log.debug("Registered service for event type ${eventType}")
		return svc.id
	}

	private String unregisterServiceMapping(String svcId, String eventType) {
		log.debug("In unregisterServiceMapping - eventType = "+eventType+" service ID = " + svcId)
		String status = "Not found"
		java.util.List svcMappings = this.services.get(eventType)
		if (svcMappings != null) {
			def mappingFound = false
			for (java.util.ListIterator<RegisteredService> iter = svcMappings.listIterator(); iter.hasNext(); ) {
				RegisteredService registeredSvc = iter.next()
				if (registeredSvc.id == svcId) {
					iter.remove()
					mappingFound = true
					break
				}
			}
			if (mappingFound) {
				this.services.put(eventType, svcMappings)
				log.debug("Unegistered service for event type " + eventType)
				status = "ok"
			}
		}
		return status
	}

	private String generateUUID() {
		UUID uuid = UUID.randomUUID()
		return uuid.toString()
	}

	class RegisteredService {
		String id
		String serviceName
		String protocol
		String serverUrl
		int port
		
		RegisteredService() {}
	}
 }
