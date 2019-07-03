@echo off
echo Setting variables for dev run of migration suite
SET clm.user=svc-rtcmigration
SET clm.password=t35T1ng411rTcM!gR@t10n
SET mr.url=http://utmvti0190:8026
SET clm.url=https://clm.cs.zionsbank.com
SET clm.pageSize=100
SET tfs.url=https://dev.azure.com
SET tfs.collection=eto-dev
SET clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w

SET tfs.project=FutureCore
SET process.name=ZionsAgile
SET tfs.projectUri={b95a29af-917f-4762-b4bc-c716e7a33b18}
SET tfs.projectFolder=FutureCore
SET tfs.isDefaultTeam=true
SET tfs.teamGuid=dbe48e2d-5113-471a-976d-eb8c1dffa7c5
SET tfs.collectionId=1bec1897-29a0-44d0-80a8-670c5ae5ef4a
set tfs.token=5ygjaomvtrq6jgtxyrbpqsvahj2kuzzq6tjxgllrc76j7cbwzp4a
::that token was in the RQM execution?

SET db.project=core
SET spring.data.mongodb.database=adomigration_dev
SET selected.checkpoint=none

::RM specific variables
SET rm.include.update=flushQueries,whereused,phases
SET rm.include.phases=requirements
SET rm.mapping.file=.\mapping\CoreRRMMapping.xml
SET rm.filter=allFilter
SET rm.tfs.areapath=FutureCore\Requirements\R3
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

SET logdir=E:\bin\batch\logs\dev
::don't need to change these regularly
SET log.rqm=%logdir%\translate_RQMtoADO.log
SET log.dng=%logdir%\translate_DNGtoADO.log
SET log.ccm=%logdir%\translate_CCMtoADO.log

::not using these items I think
SET cache.location=e:\cache
SET tfs.user=robert.huet@zionsbancorp.com

::in this window, call the script that sequences other scripts
call orchestration_master.bat
