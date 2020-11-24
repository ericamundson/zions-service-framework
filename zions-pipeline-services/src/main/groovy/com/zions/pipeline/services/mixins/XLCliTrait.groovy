package com.zions.pipeline.services.mixins

trait XLCliTrait {
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

		}
	}
}
