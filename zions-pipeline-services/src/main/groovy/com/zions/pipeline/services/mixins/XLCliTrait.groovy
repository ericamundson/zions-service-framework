package com.zions.pipeline.services.mixins

trait XLCliTrait extends CliRunnerTrait {
	def loadXLCli(File loadDir) {
		String osname = System.getProperty('os.name')
			
		if (osname.contains('Windows')) {
			InputStream istream = this.getClass().getResourceAsStream('/xl/windows/xl.exe')
			File of = new File(loadDir, 'xl.exe')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()
		} else {
			InputStream istream = this.getClass().getResourceAsStream('/xl/linux/xl')
			File of = new File(loadDir, 'xl')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()
			
			String command = '/bin/sh'
			String option = '-c'
			def arg = [ line: "${option} chmod 777 xl" ]
			run(command, "${loadDir.absolutePath}", args)
		}
	}
}
