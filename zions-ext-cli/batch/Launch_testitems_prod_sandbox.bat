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
SET tfs.project=Sandbox
SET process.name=ZionsAgile
SET tfs.projectUri={344d4a70-f0d4-4b64-a725-4b2e321dd473}
SET tfs.projectFolder=FutureCore
SET tfs.isDefaultTeam=true
SET tfs.teamGuid={7abc7bf9-9624-40cf-a511-38142b3bebd4}
SET tfs.collectionId={931ea459-abea-43db-9aed-eb489dee1e5e}
set tfs.token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Im9PdmN6NU1fN3AtSGpJS2xGWHo5M3VfVjBabyJ9.eyJuYW1laWQiOiIzNWI2N2UzYi1kMTRlLTZmNjEtYmIyYS05ZWVhZmY0MjMzMzgiLCJzY3AiOiJhcHBfdG9rZW4iLCJhdWkiOiJiOTJmYjg4OS0yYTc4LTQyZDctODNiMC0zYzg2NTRlNGJlMDUiLCJzaWQiOiI1NzM1Y2UzNi1mNWU0LTRhNjYtOWI0Ni1jMzIzZjlmYzBhZmUiLCJpc3MiOiJhcHAudnN0b2tlbi52aXN1YWxzdHVkaW8uY29tIiwiYXVkIjoiYXBwLnZzdG9rZW4udmlzdWFsc3R1ZGlvLmNvbXx2c286OTMxZWE0NTktYWJlYS00M2RiLTlhZWQtZWI0ODlkZWUxZTVlIiwibmJmIjoxNTYxNDc3NTc1LCJleHAiOjE1NjE0ODE3NzV9.A1QvNF1yWJVU3lwxyZf5sZNE2Ztp3Mrvi63LgaCLSr8vAAk2j5jj5hZlSaB7fHQUOomo9p6E0nCDi-iCiO-Pwksq825IPtLspD9bFsAlvwRGR28k-f75eRC9pKICLdR5BZM3lbpP1MCU5gXEv-wzSkt5fT17DPg5oFu1K2aNa0Alj1SlXEwBR1Vlo6Se8DYUaDZgmIHjmKYfALmxB1U-SMKiYtIvuUsbQukYOlKilOVaMKtDnIPQJ3F7eZzz9RefjEmK-qoq4iXIB-4zBnNJNTVRgLVnww3AqG37Vsxd_SeUZXupa5bghUQpPMwfF-2T8zzIYVDiWbdBqS9WVeo1_A
::that token was in the RQM execution?

SET db.project=core
SET spring.data.mongodb.database=adomigration_sand
SET selected.checkpoint=none

::RM specific variables
::blank out include.updates to skip RM artifact migration
SET rm.include.update=flushQueries,whereused,phases
SET rm.include.phases=requirements
SET rm.mapping.file=.\mapping\CoreRRMMapping.xml
SET rm.filter=allFilter
SET rm.tfs.areapath=FutureCore\Requirements\MigrationTest
SET rm.sql.resource=/sql/coretest.sql


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

SET logdir=E:\bin\batch\logs\prodsandbox
::don't need to change these regularly if you set the above folder to target environment/project
SET log.rqm=%logdir%\translate_RQMtoADO.log
SET log.dng=%logdir%\translate_DNGtoADO.log
SET log.ccm=%logdir%\translate_CCMtoADO.log

::not using these items I think
SET cache.location=e:\cache
::this should instead be the tfs.users in each application-xdb.properties
SET tfs.user=robert.huet@zionsbancorp.com

::in this window, call the script that sequences other scripts
call orchestration_master.bat
