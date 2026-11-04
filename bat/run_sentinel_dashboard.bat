@echo off
if "%SENTINEL_HOME%"=="" (
    echo SENTINEL_HOME is required.
    exit /b 1
)
if "%SENTINEL_USERNAME%"=="" (
    echo SENTINEL_USERNAME is required.
    exit /b 1
)
if "%SENTINEL_PASSWORD%"=="" (
    echo SENTINEL_PASSWORD is required.
    exit /b 1
)
cd /d "%SENTINEL_HOME%"
java -Dserver.port=8151 -Dsentinel.dashboard.auth.username=%SENTINEL_USERNAME% -Dsentinel.dashboard.auth.password=%SENTINEL_PASSWORD% -jar sentinel-dashboard-1.8.6.jar
pause