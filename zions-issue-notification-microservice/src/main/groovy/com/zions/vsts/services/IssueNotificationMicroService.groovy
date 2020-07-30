package com.zions.vsts.services

import com.zions.vsts.services.rmq.mixins.NotificationReceiverTrait
import com.zions.vsts.services.work.calculations.RollupManagementService
//import com.zions.vsts.services.ws.client.WebSocketMicroServiceTrait

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles notification of micro-service issues.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class IssueNotificationMicroService implements NotificationReceiverTrait {


}

