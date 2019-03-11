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

	private Map services = new HashMap<String,Map>()
	private Map lastUsed = new HashMap<String,Integer>()

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
		Map svcMappings = this.services.get(eventType)
		if (svcMappings == null) {
			// No services mapped for this event type
			def message = "No service mappings for event type: ${eventData.eventType}"
			log.error("${message}")
			return new ResponseEntity<String>("${message}", HttpStatus.OK)
		}
		log.debug("Found service mappings for event ${eventData.eventType}")
		ResponseEntity<String> resp = ResponseEntity.ok(HttpStatus.OK)
		svcMappings.each { svcType, pool ->
			RegisteredService regSvc = getServiceDetails(svcType, pool)
			//String svcType = regSvc.serviceType
			log.debug("Mapping is for " + svcType + " service")
			log.debug("Create URI for target service ${regSvc.serverUrl}:${regSvc.port}"+request.getRequestURI())
			URI uri = new URI("${regSvc.protocol}", null, "${regSvc.serverUrl}", regSvc.port, request.getRequestURI(), request.getQueryString(), null)
			RestTemplate restTemplate = new RestTemplate()
			log.debug("Forward request to target service ...")
			resp = restTemplate.exchange(uri, method, new HttpEntity<String>(body, createHeaders(request)), String.class)
			// at least log the failure
			if (!resp.getStatusCode().equals(HttpStatus.OK)) {
				log.error("EventController::forwardADOEvent -- Failed. Status: "+resp.getStatusLine());
			}
		}
		return resp
		//return ResponseEntity.ok(HttpStatus.OK)
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
		svc.serviceType = "${serviceData.serviceType}"
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
		String status = unregisterServiceMapping("${serviceData.svcUUID}", "${serviceData.eventType}", "${serviceData.serviceType}")
		if (status != "ok") {
			return new ResponseEntity<String>(status, HttpStatus.NOT_FOUND)
		}
		return new ResponseEntity<String>("OK", HttpStatus.OK)
	}

    private def getServiceDetails(String serviceType, List svcMappings) {
		int numMappings = svcMappings.size()
		int lastIdx = this.lastUsed.get(serviceType)
		log.debug("getServiceDetails:: Last index for service ${serviceType} = "+lastIdx)
		if (numMappings == 1) {
			log.debug("getServiceDetails:: Found a single mapping for event type ${serviceType}")
			lastIdx = 0
		} else {
			// if there are multiple mappings for same event type / serviceType, round robin the requests
			lastIdx++
			if (lastIdx >= numMappings) {
				// reset index to beginning
				lastIdx = 0
			}
		}
		RegisteredService registeredSvc = svcMappings.get(lastIdx)
		// set as last and return
		this.lastUsed.put(serviceType, lastIdx)
		log.debug("getServiceDetails:: Return service mapping ${registeredSvc.id} for service ${serviceType}")
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
				//log.debug("Setting " + hdrName + " header to: " + hdrValue)
				requestHeaders.set(hdrName,hdrValue);
			}
		}
		//log.debug("Returning Headers ...")
		return requestHeaders
    }

	private String registerServiceMapping(RegisteredService svc, String eventType) {
		String svcType = svc.serviceType
		log.debug("In registerServiceMapping - eventType = ${eventType} and service = ${svcType}")
		// generate a unique for the service mapping
		svc.id = generateUUID()
		Map svcMappings = this.services.get(eventType)
		List svcPool = null
		if (svcMappings == null) {
			svcMappings = new HashMap()
			svcPool = new java.util.ArrayList()
			svcMappings.put(svcType, svcPool)
		} else {
			svcPool = svcMappings.get(svcType)
			if (svcPool == null) {
				svcPool = new java.util.ArrayList()
				svcMappings.put(svcType, svcPool)
			}
		}
		svcPool.add(svc)
		this.services.put(eventType, svcMappings)
		// initialize last used pointer for service type
		if (this.lastUsed.get(svcType) == null) {
			this.lastUsed.put(svcType, -1)
		}
		log.debug("Registered ${svcType} service for event type ${eventType}")
		return svc.id
	}

	private String unregisterServiceMapping(String svcId, String eventType, String svcType) {
		log.debug("In unregisterServiceMapping - eventType = "+eventType+" service ID = " + svcId)
		String status = "Not found"
		Map eventMap = this.services.get(eventType)
		if (eventMap != null) {
			List svcPool = eventMap.get(svcType)
			if (svcPool != null) {
				def mappingFound = false
				for (java.util.ListIterator<RegisteredService> iter = svcPool.listIterator(); iter.hasNext(); ) {
					RegisteredService registeredSvc = iter.next()
					if (registeredSvc.id == svcId) {
						iter.remove()
						mappingFound = true
						break
					}
				}
				if (mappingFound) {
					//this.services.put(eventType, svcMappings)
					// TODO: Need to check index here and adjust lastUsed pointer if necessary
					log.debug("Unegistered service for event type ${eventType} and service ${svcType}")
					status = "ok"
				}
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
		String serviceType
		String protocol
		String serverUrl
		int port
		
		RegisteredService() {}
	}
 }
