@echo off
echo Starting services...

start "Nacos Server" /D "E:\nacos-server-2.3.2\nacos\bin\" startup.cmd -m standalone
ping -n 3 127.0.0.1 >nul

start "Nginx Server" /D "D:\nginx-1.22.0\" nginx.exe
ping -n 3 127.0.0.1 >nul

start "Redis Server" /D "E:\AAA_develop\Redis\Redis-x64-3.2.100\" redis-server.exe
ping -n 3 127.0.0.1 >nul

echo =======================================
echo All services have been started.
echo 1. Nacos (port 8848)
echo 2. Nginx (port 80)
echo 3. Redis (port 6379)
echo =======================================
echo Press any key to close this window.
pause