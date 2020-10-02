set HTTPS_PROXY=http://z091182:4878Middy002@172.18.4.115:8080
az login -u eric.amundson2@zionsbancorp.com -p 4878Middy002 
az container delete -g NetworkWatcherRG --name zions-vsts-websocket-microservice -y
az logout