@echo off
echo Setting variables for dev run of migration suite
SET clm.user=svc-rtcmigration
SET clm.password=t35T1ng411rTcM!gR@t10n
SET mr.url=http://utmvti0190:8026
SET clm.url=https://clm.cs.zionsbank.com
SET clm.pageSize=100
SET clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w

SET tfs.url=https://dev.azure.com
SET tfs.collection=ZionsETO
SET tfs.project=BaNCS
SET process.name=ZionsAgile
SET tfs.projectUri={f5b48f08-0085-4814-875a-a3dbc9736967}
SET tfs.projectFolder=FutureCore
SET tfs.isDefaultTeam=true
SET tfs.teamGuid={dbc2b591-13fc-4dcf-bfdd-7f31e31a7323}
SET tfs.collectionId={931ea459-abea-43db-9aed-eb489dee1e5e}
set tfs.token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Im9PdmN6NU1fN3AtSGpJS2xGWHo5M3VfVjBabyJ9.eyJuYW1laWQiOiIzNWI2N2UzYi1kMTRlLTZmNjEtYmIyYS05ZWVhZmY0MjMzMzgiLCJzY3AiOiJhcHBfdG9rZW4iLCJhdWkiOiI3ZGRmNWVjYi0wY2VkLTRkZGUtYjUyNi01OTBlYmFhYTBmM2YiLCJzaWQiOiIxNjllZWJkNS1jYTQxLTQwMjctYWYxYS0yOGZlNTY3YTA0MTkiLCJpc3MiOiJhcHAudnN0b2tlbi52aXN1YWxzdHVkaW8uY29tIiwiYXVkIjoiYXBwLnZzdG9rZW4udmlzdWFsc3R1ZGlvLmNvbXx2c286OTMxZWE0NTktYWJlYS00M2RiLTlhZWQtZWI0ODlkZWUxZTVlIiwibmJmIjoxNTYxNzYyMjM5LCJleHAiOjE1NjE3NjY0Mzl9.WZFCGgN58DaH4NsBgvE0ScjJEDxNll8Bg2lRlVlW56o5n4EFRRuhJcC0oB2Xp-AVRBDK5ULRrfchc-4mdI4l-MwCgo9u_t3TbVBYhIvJX7M147A9PE-PlgMF8DwjeiY_CW114ZHX1Wk8z2_g8aBRCm0pyu3uPMZDhvpb_vCFbuthy8qBgYniW7r9TuTfrRC1Odiq8XJcVJj1cITIxmaJ02O4aiuKehf8iDTCxFHITzgnZn0kHjh6sDUNcfW5LlqV3ByF6Su_RUBJWu0SRLzCqWSzdwBNqGOtBRPQDg7PuDGhOsMqMI-XGYWSjj2HgpDRCF_EucN0QNJHD9FzUY0Lsg
::that token was in the RQM execution?

SET db.project=core
SET spring.data.mongodb.database=adomigration_prod
SET selected.checkpoint=priorToLogEntries

::RM specific variables
::blank out include.updates to skip RM artifact migration
::If updating, use flushQueriesDelta instead of flushQueries in rm.include.update
::and use the update sql file instead of the standard one
SET rm.include.update=phases
SET rm.include.phases=requirements
SET rm.mapping.file=.\mapping\CoreRRMMapping.xml
SET rm.filter=allFilter
SET rm.tfs.areapath=BaNCS\Requirements\R3
SET rm.sql.resource=sql/core.sql


::CCM specific variables
::blank out include.updates to skip CCM work item migration
SET ccm.include.update=
SET ccm.include.phases=
SET wi.query="workitem/workItem[projectArea/name='Zions FutureCore Program (Change Management)']/(id|modified|state/group|target/archived|target/name|type/name|parent/state/group|parent/target/archived|parent/type/name|related/state/group|related/target/archived|related/type/name)"
SET ccm.projectArea="Zions FutureCore Program (Change Management)"
SET ccm.template.dir="e:\bin\batch\templates"
SET wit.mapping.file=".\CoreCCMMapping.xml"
SET wi.filter=allFilter

::QM specific variables
::blank out include.updates to skip test artifact migration
SET qm.include.update=
SET qm.include.phases=
SET rqm.projectArea="Zions FutureCore Program (Quality Management)"
SET rqm.template.dir="e:\bin\batch\templates\qm_templates"
SET rqm.mapping.file="e:/bin/batch/mapping/CoreRQMMapping.xml"
SET rqm.query="none"
SET rqm.filter=allFilter
SET rqm.tfs.areapath="FutureCore\\Test"

SET logdir=E:\bin\batch\logs\prodcore
::don't need to change these regularly if you set the above folder to target environment/project
SET log.rqm=%logdir%\translate_RQMtoADO.log
SET log.dng=%logdir%\translate_DNGtoADO_checkpointrestart.log
SET log.ccm=%logdir%\translate_CCMtoADO.log

::not using these items I think
SET cache.location=e:\cache
::this should instead be the tfs.users in each application-xdb.properties
SET tfs.user=robert.huet@zionsbancorp.com

::in this window, call the script that sequences other scripts
call orchestration_master.bat
