java -Dspring.profiles.active=qmdb -jar e:\batch\zions-ext-cli.jar translateRQMToMTM --clm.url=https://clm.cs.zionsbank.com --clm.user=z091182  --tfs.url=https://dev.azure.com/zionseto --tfs.user=z091182 --tfs.token=5ygjaomvtrq6jgtxyrbpqsvahj2kuzzq6tjxgllrc76j7cbwzp4a --clm.projectArea="Zions FutureCore Program (Quality Management)" --qm.template.dir="d:/bin/batch/templates/qm_templates" --test.mapping.file="d:/bin/batch/mapping/CoreRQMMapping.xml" --tfs.project="FutureCore" --process.name="ZionsAgile" --qm.query="none" --qm.filter=allFilter --tfs.areapath="FutureCore\\Test" --cache.location=d:/futurecore_test_cache --selected.checkpoint=none  --db.project=core --spring.data.mongodb.database=adomigrate --logging.file=e:\bin\batch\logs\core_rqmtoado.log %*