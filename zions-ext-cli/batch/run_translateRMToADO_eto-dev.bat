java -Dspring.profiles.active=rmdb -jar d:\bin\batch\zions-ext-cli.jar translateRmBaseArtifactsToADO  --include.update=flushQueries,phases --include.phases=requirements --clm.user=svc-rtcmigration --clm.password=t35T1ng411rTcM!gR@t10n  --mr.url=http://utmvti0190:8026 --clm.url=https://clm.cs.zionsbank.com  --clm.pageSize=100 --tfs.url=https://dev.azure.com --tfs.collection=eto-dev --tfs.user=robert.huet@zionsbancorp.com --clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w --rm.mapping.file=./CoreRRMMapping.xml --tfs.project=FutureCore --process.name=ZionsAgile --rm.filter=allFilter --tfs.areapath=FutureCore\Requirements\R3 --tfs.projectUri={b95a29af-917f-4762-b4bc-c716e7a33b18} --tfs.projectFolder=FutureCore --tfs.isDefaultTeam=true --tfs.teamGuid=dbe48e2d-5113-471a-976d-eb8c1dffa7c5 --tfs.collectionId=1bec1897-29a0-44d0-80a8-670c5ae5ef4a --db.project=core --spring.data.mongodb.database=adomigration_dev --selected.checkpoint=last --logging.file=c:\\logs7\\rm.log