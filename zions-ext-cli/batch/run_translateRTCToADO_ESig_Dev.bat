java -Dspring.profiles.active=clm -jar e:\batch\zions-ext-cli.jar translateRTCWorkToVSTSWork --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182 --clm.password="4878Middy002" --tfs.url=https://dev.azure.com/eto-dev --tfs.user=z091182 --tfs.token=nne526gq4vgseefkdn25v4cw2pm74qsacn2ylkhlqjltrd4oalvq --clm.projectArea="eSignature (Change Management)" --ccm.template.dir="e:\esig_wit_templates" --wit.mapping.file="./eSigCCMMapping.xml" --tfs.project="eSignature" --process.name="DTSTest" --include.update=clean,phases --include.phases=workdata,worklinks,attachments --wi.query="workitem/workItem[projectArea/name='eSignature (Change Management)']/(id|modified|state/group|target/name|target/archived|type/name|category/name|related/state/group|related/type/name|related/category/name|parent/state/group|parent/type/name|parent/category/name)" --wi.filter=allFilter --cache.location=d:/eSignature_cache --selected.checkpoint=none --tfs.areapath="eSignature"