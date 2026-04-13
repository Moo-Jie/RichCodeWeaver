@echo off 
title prometheus
color 1b
cd /d "E:\prometheus-3.7.3.windows-amd64"
prometheus.exe --config.file=prometheus.yml
exit 