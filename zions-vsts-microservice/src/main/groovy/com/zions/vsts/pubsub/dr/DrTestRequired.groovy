package com.zions.vsts.pubsub.dr

interface DrTestRequired {
	boolean requiresSendToDR(def adoEvent);
	String getType();
}
