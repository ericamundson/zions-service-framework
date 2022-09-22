set HTTPS_PROXY=http://z091182:4878Middy002@proxy.cs.zionsbank.com:8080
az login -u eric.amundson2@zionsbancorp.com -p 4878Middy002 
az container delete -g NetworkWatcherRG --name zions-vsts-websocket-microservice -y
az logout