cd ..

cd ruoyi-api
call mvn deploy
cd ../ruoyi-common
call mvn deploy
cd ../zlt-api
call mvn deploy
cd ../zlt-common
call mvn deploy
cd ../zlt-modules
call mvn deploy
pause