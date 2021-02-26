package com.zions.pipeline.services.feedback

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.pipeline.services.db.PipelineLogItemRepository
import com.zions.pipeline.services.db.LogType
import com.zions.pipeline.services.db.PipelineLogItem
import com.zions.pipeline.services.feedback.model.LogNode

import com.zions.vsts.services.extdata.ExtensionDataManagementService

import groovy.util.logging.Slf4j

@Component
@Slf4j
class FeedbackService {
	@Autowired
	PipelineLogItemRepository pipelineLogItemRepository
	
	@Autowired
	ExtensionDataManagementService extensionDataManagementService
	
	void logContextStart(String pid, String message) {
		addComposeAndSend(LogType.ContextStart, pid, message)		
	}
	void logContextComplete(String pid, String message) {
		addComposeAndSend(LogType.ContextComplete, pid, message)
	}

	void logInfo(String pid, String message) {
		addComposeAndSend(LogType.Info, pid, message)
	}
	
	void logWarn(String pid, String message) {
		addComposeAndSend(LogType.Warn, pid, message)
	}
	
	void logError(String pid, String message) {
		addComposeAndSend(LogType.Error, pid, message)
	}
	
	void logFailed(String pid, String message) {
		addComposeAndSend(LogType.Failed, pid, message)
	}
	
	void logCompleted(String pid, String message) {
		addComposeAndSend(LogType.Completed, pid, message)
	}

	void addComposeAndSend(LogType type, String pid, String message) {
		String extId = "COE_${pid}"
		List<PipelineLogItem> logs = pipelineLogItemRepository.findByPipelineId(pid)
		int lsize = logs.size()
		List<String> context = []
		int cSize = 0
		if (lsize > 0) {
			 context = logs[lsize-1].contexts.clone()
			 cSize = context.size()
		}
		if (type == LogType.ContextStart) {
			context.add(message)
			cSize = context.size()
		}
		if (cSize > 0 && type == LogType.ContextComplete) {
			context.remove(cSize-1)
		}
		String lType = "${type}"
		// add
		PipelineLogItem item = [timestamp: new Date().toString(), logType: lType, pipelineId: pid, log: message, contexts: context, name:'']
		pipelineLogItemRepository.save( item )
		
		//compose
		logs.add(item)
		
		
		//send
		if (item.logType == 'ContextComplete' || item.logType == 'Failed' || item.logType == 'Completed') {
			List<LogNode> nodes = buildLogNodes(logs)
		
			def extData = [id: extId, nodes: nodes]
			extensionDataManagementService.ensureExtensionData(extData)
		}
	}
	
	private List<LogNode> buildLogNodes(List<PipelineLogItem> logs) {
		List<LogNode> nodes = []
		Map<String, LogNode> nodeMap = [:]
		for (PipelineLogItem item in logs) {
			if (item.logType == 'ContextStart') {
				LogNode node = new LogNode( name: item.log, logItems: [], nodes: [] )
				if (item.contexts.size() == 1) {
					nodes.add(node)
					nodeMap[item.log] = node
				} else {
					int cs = item.contexts.size()
					String context = item.contexts[cs-2]
					LogNode parent = nodeMap[context]
					if (parent) {
						parent.nodes.add(node)
						nodeMap[item.log] = node
					}
				}
			} 
			if (!item.logType.startsWith('Context')) {
				int cs = item.contexts.size()
				if (cs > 0) {
					String context = item.contexts[cs-1]
					LogNode parent = nodeMap[context]
					if (parent) {
						parent.logItems.add(item)
					}
				}

			}
			
		}
		return nodes
	}
}
