package com.zions.auto.base

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import groovy.util.logging.Slf4j

@Component
@Slf4j
class Fiddler {
	@Value('${fiddler.app:}')
	String fiddlerApp
	@Value('${fiddler.cli:}')
	String fiddlerCli
	@Value('${fiddler.dump:}')
	String fiddlerDump
	Process fiddlerProcess

	boolean hasDump = false
		
	public open() {
		fiddlerProcess = new ProcessBuilder(fiddlerApp).start()
		Thread.sleep(2000)
	}
	public close() {
		stop()
		Thread.sleep(1000)
		clear()
		Thread.sleep(1000)
		fiddlerProcess.destroyForcibly()
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
		if (!fiddlerCli) 
			return null
		else
			return new ProcessBuilder(fiddlerCli,command).start()
	}
}
