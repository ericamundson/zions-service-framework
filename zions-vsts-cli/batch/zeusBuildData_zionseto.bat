java -Dspring.profiles.active=default -jar ..\libs\zions-vsts-cli.jar zeusBuildData  --tfs.url=https://dev.azure.com/zionseto --tfs.user=%ADO_USER% --tfs.token=%ADO_TOKEN% --tfs.project="Zeus" %*