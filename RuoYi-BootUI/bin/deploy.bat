@echo off

cd ..
cd zlt-modules
call mvn clean deploy -Dmaven.test.skip=true

cd ..
cd zlt-frame-starter
call mvn deploy -Dmaven.test.skip=true

cd ..
cd ruoyi-admin
call mvn deploy -Dmaven.test.skip=true

pause