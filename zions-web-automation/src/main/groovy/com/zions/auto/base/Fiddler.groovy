package com.zions.auto.base

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component
@Slf4j
class Fiddler {
	@Value('${fiddler.exe:}')
	String fiddlerExe
	@Value('${fiddler.dump:}')
	String fiddlerDump

	boolean hasDump = false
		
	public Fiddler() {
		
	}
	public start() {
		exec("start")
	}
	public stop() {
		exec("stop")
	}
	public dump() {
		if (fiddlerDump && exec("dump"))
			hasDump = true
	}
	public clear() {
		exec("clear")
	}
	private exec(String command) {
		if (!fiddlerExe) 
			return null
		else
			return new ProcessBuilder(fiddlerExe,command).start()
	}
}
