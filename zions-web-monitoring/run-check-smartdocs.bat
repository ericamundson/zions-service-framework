@echo off
set drive=C:
set home_dir=SmartDocMonitoring
set monitor_home=%drive%\%home_dir%\
set log_file=%drive%\logs\SmartDocLivenessCheck.log
%drive%
CD %monitor_home%
echo monitor_home=%monitor_home%
java -Dspring.profiles.active=smartdoc -Dproxy.Host=172.18.4.115 -Dproxy.Port=8080 -Dproxy.User= -Dproxy.Password= -jar .\build\libs\zions-web-monitoring-latest.jar monitorSmartDoc --cache.dir=%monitor_home% --tfs.url=https://dev.azure.com --tfs.project=DTS --tfs.collection=ZionsETO --tfs.areapath="DTS\ALMOps\ModernRequirements\Bug" --tfs.owner=robert.huet@zionsbancorp.com --mr.url=https://dev.azure.com/ZionsETO/DTS/_apps/hub/edevtech-mr.iGVSO-OnPrem-mrserviceus1008.subHubWork-SmartDocs-OnPrem --mr.smartdoc.name=TestDoc --mr.haslicense=true --logging.level.root=INFO --logging.file=%log_file%
exit