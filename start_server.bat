@echo off
echo 启动聊天服务器...
cd /d "%~dp0"
java -cp . src.ChatServer
pause
