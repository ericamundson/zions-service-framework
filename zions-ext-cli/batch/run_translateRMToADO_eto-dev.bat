java -Dspring.profiles.active=rmdb -jar d:\bin\batch\zions-ext-cli.jar translateRmBaseArtifactsToADO  --include.update=phases --include.phases=requirements --mr.url=http://utmvti0190:8026  --clm.url=https://clm.cs.zionsbank.com --clm.user=svc-rtcmigration --clm.password=t35T1ng411rTcM!gR@t10n  --clm.pageSize=100 --tfs.url=https://dev.azure.com --tfs.collection=eto-dev --tfs.user=robert.huet@zionsbancorp.com  --clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w --rm.mapping.file=./CoreRRMMapping.xml --tfs.project=FutureCore --process.name=ZionsAgile --include.update=data --oslc.namespaces="&oslc.prefix=dcterms=<http://purl.org/dc/terms/>&oslc.prefix=nav=<http://jazz.net/ns/rm/navigation%23>&oslc.prefix=rdf=<http://www.w3.org/1999/02/22-rdf-syntax-ns%23>&oslc.prefix=rmTypes=<http://www.ibm.com/xmlns/rdm/types/>&oslc.prefix=rm=<http://www.ibm.com/xmlns/rdm/rdf/>" --oslc.select="&oslc.select=dcterms:modified,dcterms:identifier,rmTypes:ArtifactFormat" --oslc.where="&oslc.where=nav:parent=<https://clm.cs.zionsbank.com/rm/folders/_QMhwgVsGEemdeebbT-QcUQ>" --rm.filter=allFilter --tfs.areapath=FutureCore\Requirements\R3 --tfs.projectUri={b95a29af-917f-4762-b4bc-c716e7a33b18} --tfs.projectFolder=FutureCore --tfs.isDefaultTeam=true --tfs.teamGuid=dbe48e2d-5113-471a-976d-eb8c1dffa7c5 --tfs.collectionId=1bec1897-29a0-44d0-80a8-670c5ae5ef4a --db.project=core --spring.data.mongodb.database=adomigration_dev