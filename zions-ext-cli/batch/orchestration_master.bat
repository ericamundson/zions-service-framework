@echo off

echo Launching migration of base work items from RTC into ADO
start "RTC to ADO" .\orchestrated_translateRTCtoADO.bat

echo Launching migration of base quality objects from QM into ADO
start "RQM to ADO" .\orchestrated_translateRQMtoADO.bat

echo Launching migration of base requirements from DNG into ADO
start "DNG artifacts to ADO" /wait .\orchestrated_translateDNGtoADO.bat

::due to /wait parameter above, this will not run until artifacts are migrated.  Maybe we'll run it seperate.
echo (theoretically) Launching migration of modules from DNG into ADO

::likewise this should wait on *something*, I assume DNG artifacts as they take the longest
echo (theoretically) Linking all work items within ADO