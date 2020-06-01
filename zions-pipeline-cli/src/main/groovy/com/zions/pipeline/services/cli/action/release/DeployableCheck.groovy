package com.zions.pipeline.services.cli.action.release

import org.springframework.boot.ApplicationArguments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.common.services.cli.action.CliAction
import com.zions.xld.services.ci.CIService
import static groovy.io.FileType.*

@Component
@Slf4j
class DeployableCheck implements CliAction {

	@Value('${scan.dir:}')
	String scanDirName


	@Value('${package.template:}')
	String packageTemplate

	@Value('${out.dir:}')
	String outDir

	@Value('${xlw.location:}')
	String xlwLocation

	@Value('${xld.url:https://xldeploy.cs.zionsbank.com}')
	String xldUrl

	@Value('${xlr.url:https://xlrelease.cs.zionsbank.com}')
	String xlrUrl

	@Value('${xl.user:z091182}')
	String xlUser

	@Value('${xl.password:dummy}')
	String xlPassword

	@Value('${file.filter:}')
	String fileFilter

	@Value('${version.regex:}')
	String versionRegex

	@Value('${file.count:-1}')
	int fileCount

	@Value('${package.yaml.path:}')
	File packageYamlFile

	@Value('${xld.application.name:}')
	String xldApplicationName

	@Value('${xld.application.shortname:}')
	String xldApplicationShortName

	@Value('${file.filter.type:regex}')
	String fileFilterType

	@Value('${create.release.flag:true}')
	boolean createRelease

	@Value('${scan.full:false}')
	boolean scanFull

	@Autowired
	CIService cIService



	String packageYaml = '''---
apiVersion: xl-deploy/v1
kind: Applications
spec:
# deployable folder:  /app/depot
# script folder: /app
- directory: Applications/UOB
  children:
  - name: UOB_Deploy
    type: udm.Application
    pipeline: Configuration/UOB/Component_Pipeline
    children:
    - name: %%app_version%%
      type: udm.DeploymentPackage
      deployables:
      - name: package_folder
        type: file.Folder
        targetPath: '{{deploy.path}}'
        createTargetPath: "true"
        file: !file '%in_folder%'
      - name: run perl script
        type: advcmd.Command
        command: 'ZIONS_%app_version% | {{script.path}}/AppInstall_arlm-v6.pl'
        createOrder: "62"
        destroyOrder: "42"
        alwaysRun: "true"
        noopOrder: "62"
'''

	String importPackageYaml = '''apiVersion: xl/v1
kind: Import
metadata:
  imports:
    - xld-package.yaml
'''
	String releaseYaml = '''apiVersion: xl/v1
kind: Import
metadata:
  imports:
    - xebialabs/xlr-release.yaml
'''

	public def execute(ApplicationArguments data) {
		File importPackageFile = new File("${xlwLocation}\\xl-importpackage.yaml")
		def oip = importPackageFile.newDataOutputStream()
		oip << importPackageYaml
		oip.close()
		File releaseFile = new File("${xlwLocation}\\xl-create-release.yaml")
		oip = releaseFile.newDataOutputStream()
		oip << releaseYaml
		oip.close()

		if (packageYamlFile.exists()) {
			packageYaml = packageYamlFile.text
		}
		def filePattern = null
		if (fileFilterType == 'regex') {
			filePattern = Pattern.compile(fileFilter)
		} else {
			filePattern = fileFilter
		}
		File scanDir = new File(scanDirName)
		def filesPath=[]
		//		scanDir.eachFileRecurse(FILES) {  File file ->
		//			Matcher fullNameMatch = "${file.name}" =~ ~fileFilter
		//			if (fullNameMatch.find()) {
		//				filesPath.add(file.path)
		//				Pattern p = Pattern.compile(versionRegex)
		//				Matcher m = "${file.name}" =~ ~versionRegex
		//				if (!m.find()) return
		//				String packageName = "${file.name}"
		//				packageName = packageName.substring(m.start(), m.end())
		//				DeployableCheck.log.info "Package: ${packageName}"
		//				String hPackageCIPath = "${xldApplicationName}/${packageName}"
		//				def pCI = cIService.getCI(hPackageCIPath)
		//				if (!pCI) {
		//					def files = []
		//					files.add(file)
		//					relatedFiles(file, files)
		//					//File oDir = new File(outDir)
		//					buildFolder(packageName, outDir, files)
		//					createPackage(packageName, outDir)
		//				}
		//
		//			}
		//
		//		}
		if (scanFull) {
			scanDir.eachDirRecurse() { File dir ->
				dir.eachFileMatch(filePattern) { File file ->
					DeployableCheck.log.info "${file.path}"
					filesPath.add(file.path)
					Pattern p = Pattern.compile(versionRegex)
					Matcher m = "${file.name}" =~ ~versionRegex
					if (!m.find()) return
						String packageName = "${file.name}"
					packageName = packageName.substring(m.start(), m.end())
					DeployableCheck.log.info "Package: ${packageName}"
					String hPackageCIPath = "${xldApplicationName}/${packageName}"
					def pCI = cIService.getCI(hPackageCIPath)
					if (!pCI) {
						def files = []
						files.add(file)
						relatedFiles(file, files)
						//File oDir = new File(outDir)
						buildFolder(packageName, outDir, files)
						createPackage(packageName, outDir)
					}
				}
			}

		} else {
			scanDir.eachFileMatch(filePattern) { File file ->
				DeployableCheck.log.info "${file.path}"
				filesPath.add(file.path)
				Pattern p = Pattern.compile(versionRegex)
				Matcher m = "${file.name}" =~ ~versionRegex
				if (!m.find()) return
					String packageName = "${file.name}"
				packageName = packageName.substring(m.start(), m.end())
				DeployableCheck.log.info "Package: ${packageName}"
				String hPackageCIPath = "${xldApplicationName}/${packageName}"
				def pCI = cIService.getCI(hPackageCIPath)
				if (!pCI) {
					def files = []
					files.add(file)
					relatedFiles(file, files)
					//File oDir = new File(outDir)
					buildFolder(packageName, outDir, files)
					createPackage(packageName, outDir)
				}
			}
			scanDir.eachDir() { dir ->
				dir.eachFileMatch(filePattern) { File file ->
					DeployableCheck.log.info "${file.path}"
					filesPath.add(file.path)
					Pattern p = Pattern.compile(versionRegex)
					Matcher m = "${file.name}" =~ ~versionRegex
					if (!m.find()) return
						String packageName = "${file.name}"
					packageName = packageName.substring(m.start(), m.end())
					DeployableCheck.log.info "Package: ${packageName}"
					String hPackageCIPath = "${xldApplicationName}/${packageName}"
					def pCI = cIService.getCI(hPackageCIPath)
					if (!pCI) {
						def files = []
						files.add(file)
						relatedFiles(file, files)
						//File oDir = new File(outDir)
						buildFolder(packageName, outDir, files)
						createPackage(packageName, outDir)
					}
				}
			}
		}
	}

	def relatedFiles(File file, def files) {
		//		String patchFileName = "${file.path}".replace('dms', 'patches')
		//		File pFile = new File(patchFileName)
		//		if (pFile.exists()) {
		//			files.add(pFile)
		//		}

	}

	def buildFolder(String packageName, String outDir, def files) {
		File packageOut = new File("${outDir}/${packageName}")
		packageOut.mkdirs()
		if (files.size() > 0) {
			new AntBuilder().copy( todir: "${outDir}/${packageName}", overwrite: true ) {
				fileset( dir: "${files[0].parentFile.path}" ) {
					files.each { afile ->
						include( name: "${afile.name}")
					}
				}
			}
		}


	}

	def createPackage(String packageName, outDir) {

		String inFolder = "${outDir}\\${packageName}"
		inFolder = inFolder.substring(xlwLocation.length()+1)
		String appVersion = "${packageName}"
		String packageOut = packageYaml.replace('%in_folder%', inFolder)
		packageOut = packageOut.replace('%app_version%', appVersion)
		packageOut = packageOut.replace('!value "app_version"', appVersion)
		File packageFile = new File("${xlwLocation}\\xld-package.yaml")
		def po = packageFile.newDataOutputStream()
		po << packageOut
		po.close()

		new AntBuilder().exec(dir: "${xlwLocation}", executable: 'cmd', failonerror: true) {
			arg( line: "/c xlw.bat apply -f xl-importpackage.yaml --xl-deploy-url ${xldUrl} --xl-deploy-username ${xlUser} --xl-deploy-password ${xlPassword}")
		}

		if (createRelease) {
			//println "XLR Url: ${xlrUrl}"
			log.info "Creating new release: ${packageName}"
			new AntBuilder().exec(dir: "${xlwLocation}", executable: 'cmd', failonerror: false) {
				arg( line: "/c xlw.bat apply -f xl-create-release.yaml --xl-release-url ${xlrUrl} --xl-release-username ${xlUser} --xl-release-password ${xlPassword} --values app_version=${packageName},release_name=\"${xldApplicationShortName}-${packageName}\"")
			}

		}
	}

	public Object validate(ApplicationArguments args) throws Exception {

	}
}
