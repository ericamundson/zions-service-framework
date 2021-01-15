package com.zions.pipeline.services.mixins
import org.slf4j.Logger
trait CliRunnerTrait {
	def run(String command, String dir, def iarg, def ienv = null, Logger log = null) {
		if (log && iarg.line) {
			log.info( "cmd: ${command} args: ${iarg.line}")
		}
		AntBuilder ant = new AntBuilder()
		ant.exec(outputproperty:"text",
			 errorproperty: "error",
			 resultproperty: "exitValue",
			 dir: "${dir}",
			 executable: "${command}",
			 failonerror: false) {
			if (ienv) {
				env( ienv )
			}
			arg( iarg )
		}
		
		def result = new Expando(
			text: ant.project.properties.text,
			error: ant.project.properties.error,
			exitValue: ant.project.properties.exitValue as Integer,
			toString: { text }
		)
		
		if (result.exitValue != 0) {
			throw new Exception("""command failed with ${result.exitValue}
error: ${result.error}
text: ${result.text}""")
		} else if (log) {
			log.info(result.text)
		}

	}
}
