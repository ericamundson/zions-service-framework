:psrepeat
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=refresh,phases --include.phases=plans --qm.query="feed/entry/content/testplan[category/@value = 'PS']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :psrepeat
:psrepeatlinks
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=phases --include.phases=links --qm.query="feed/entry/content/testplan[category/@value = 'PS']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :psrepeatlinks

:r2repeat
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=refresh,phases --include.phases=plans --qm.query="feed/entry/content/testplan[category/@value = 'Release Two' or category/@value = 'Release TwoDOTFive' or category/@value = 'Release TwoDOTNine' or category/@value = 'Release TwoDOTTwo']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :r2repeat
:r2repeatlinks
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=refresh,phases --include.phases=links --qm.query="feed/entry/content/testplan[category/@value = 'Release Two' or category/@value = 'Release TwoDOTFive' or category/@value = 'Release TwoDOTNine' or category/@value = 'Release TwoDOTTwo']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :r2repeatlinks

:r1repeat
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=refresh,phases --include.phases=plans --qm.query="feed/entry/content/testplan[category/@value = 'Release One' or category/@value = 'Release OneDOTOne']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :r1repeat
:r1repeatlinks
java -Dspring.profiles.active=qmdb -jar .\zions-ext-cli-latest.jar translateRQMToADOForCore --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182  --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:\qm_templates" --test.mapping.file="./CoreRQMMapping.xml" --tfs.project="BaNCS" --process.name="ZionsAgile" --include.update=refresh,phases --include.phases=links --qm.query="feed/entry/content/testplan[category/@value = 'Release One' or category/@value = 'Release OneDOTOne']/*" --qm.filter=allFilter --tfs.areapath="BaNCS" --cache.location=/bancs_cache_prod --selected.checkpoint=none --spring.data.mongodb.database=adomigration_prod --db.project=core --parent.plan.name="FC PS Migrated (From RQM)" --process.full.plan=true --ext.url=https://extmgmt.dev.azure.com/zionseto --ext.collection=zionseto --rm.area.path="BaNCS\Requirements\R3" --email.recipient.address="eaamund@gmail.com" --execution.checkpoint=true --check.updated=true --refresh.run=true --update.cache.only=true --plan.checkpoint=true || goto :r1repeatlinks

echo !Success