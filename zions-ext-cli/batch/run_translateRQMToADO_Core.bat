java -Dspring.profiles.active=clmdb -Dproxy.Host=172.18.4.115  -Dproxy.Port=8080 -Dproxy.User=z091182 -Dproxy.Password=4878Middy001 -jar d:\batch\zions-ext-cli.jar translateRQMToMTM --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182 --tfs.token= --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="FutureCore" --process.name="ZionsAgile" --qm.query="none" --qm.filter=allFilter --tfs.areapath="FutureCore\\Test" --cache.location=d:/futurecore_test_cache --selected.checkpoint=none  --db.project=core --spring.data.mongodb.database=adomigrate %*