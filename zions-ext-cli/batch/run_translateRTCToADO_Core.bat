java -Dspring.profiles.active=default -Dproxy.Host=172.18.4.115  -Dproxy.Port=8080 -Dproxy.User=z091182 -Dproxy.Password=4878Middy001 -jar d:\batch\zions-ext-cli.jar translateRTCWorkToVSTSWork --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182 --clm.password="4878Middy001" --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182 --tfs.token=5ygjaomvtrq6jgtxyrbpqsvahj2kuzzq6tjxgllrc76j7cbwzp4a --clm.projectArea="Zions FutureCore Program (Change Management)
" --ccm.template.dir="d:\batch\templates" --wit.mapping.file="./CoreMapping.xml" --tfs.project="FutureCore" --process.name="DTSTest" --include.update=phases --include.phases==data,links,attachments --wi.query="workitem/workItem[projectArea/name='Zions FutureCore Program (Change Management)
']/(id|modified|state/group|target/archived|target/name|type/name|parent/state/group|parent/target/archived|parent/type/name|related/state/group|related/target/archived|related/type/name)" --wi.filter=allFilter --cache.location=d:/zionsetocorecache --db.project=core