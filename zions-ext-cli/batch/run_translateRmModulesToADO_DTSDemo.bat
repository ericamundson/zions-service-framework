java -Dspring.profiles.active=rmdb -jar d:\bin\batch\zions-ext-cli.jar translateRmBaseArtifactsToADO --include.update=whereused,phases --clm.url=https://clm.cs.zionsbank.com --clm.user=svc-rtcmigration --clm.password=t35T1ng411rTcM!gR@t10n  --clm.pageSize=10 --tfs.url=https://dev.azure.com --tfs.collection=DTSDemo --tfs.user=robert.huet@zionsbancorp.com --tfs.token=ahv4wcu7cqnbnz7c6refxp2o2evh3phlqyead7ogbw72ohxqessa --clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w --rm.mapping.file=./CoreRRMMapping.xml --tfs.project=BaNCS --process.name=ZionsAgile --rm.query="collectionURI=_Z2_j8fQqEeihN7TNly_siw" --rm.filter=allFilter --tfs.areapath=BaNCS\Requirements --mr.url=http://utmvti0190:8026 --mr.template=Basic --mr.folder=/ --tfs.altuser=rbhuet --tfs.altpassword=Asc3ndant --tfs.projectUri={e8bf5f47-4a90-4f95-9aa3-50a64bd9e46f}  --tfs.projectFolder=BaNCS --tfs.isDefaultTeam=true --tfs.teamGuid=ed3918ba-7319-46df-9349-036e49294aaf --tfs.collectionId=078a5551-2ee5-4244-91ef-2fae98fa2f62 --spring.data.mongodb.database=adomigration_DTSDemo --logging.level.com.zions.common.services.rest.AGenericRestClient=DEBUG --logging.file=c:\\logs\\translateRmModulesToADO.log --cache.module=RM