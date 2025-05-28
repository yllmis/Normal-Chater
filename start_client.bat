@echo off
echo 启动聊天客户端...
cd /d "%~dp0"
java -cp . src.ChatClient
pause
