@echo on
set drive=C:
set home_dir=SmartDocMonitoringTest
set monitor_home=%drive%\%home_dir%\
set sel_driver=%drive%\chrome-86\chromedriver.exe
set log_file=%drive%\logs\SmartDocLivenessCheckTest.log
set fiddler_app=%drive%\Program Files\Fiddler\Fiddler.exe
set fiddler_cli=%drive%\Program Files\Fiddler\ExecAction.exe
set fiddler_dump=%drive%\Fiddler2\Captures\dump.saz
%drive%
CD %monitor_home%
echo monitor_home=%monitor_home%
java -Dspring.profiles.active=mrauto -Dproxy.Host=172.18.4.115 -Dproxy.Port=8080 -Dproxy.User= -Dproxy.Password= -jar .\build\libs\zions-web-automation-latest.jar monitorSmartDoc --cache.dir=%monitor_home% --tfs.url=https://dev.azure.com --tfs.project=ALMOpsTest --tfs.collection=eto-dev --tfs.areapath="ALMOpsTest" --tfs.owner=robert.huet@zionsbaAncorp.com --mr.url=https://dev.azure.com/eto-dev/ALMOpsTest/_apps/hub/edevtech-mr.iGVSO-OnPrem-SandBox-002.subHubWork-SmartDocs-OnPrem --sel.timeout.sec=90 --mr.smartdoc.name=TestDoc --mr.haslicense=true --sel.driver="%sel_driver%" --logging.level.root=INFO --logging.file="%log_file%" --fiddler.cli="%fiddler_cli%" --fiddler.app="%fiddler_app%" --fiddler.dump="%fiddler_dump%"
pause
exit