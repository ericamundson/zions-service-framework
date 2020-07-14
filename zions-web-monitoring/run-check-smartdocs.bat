D:
cd "D:\git\zions-service-framework\zions-web-monitoring"
java -Dspring.profiles.active=smartdoc -jar D:\git\zions-service-framework\zions-web-monitoring\build\libs\zions-web-monitoring-latest.jar monitorSmartDoc --tfs.url=https://dev.azure.com --tfs.project=DTS --tfs.collection=ZionsETO --tfs.areapath="DTS\ALMOps\ModernRequirements" --mr.url=https://dev.azure.com/ZionsETO/DTS/_apps/hub/edevtech-mr.iGVSO-OnPrem-mrserviceus1008.subHubWork-SmartDocs-OnPrem --mr.smartdoc.name=TestDoc --mr.haslicense=true"
exit