@echo off
echo Starting Lendex...
docker-compose up -d
echo.
echo App is running!
echo Your friends can access it at: http://%COMPUTERNAME%:3000
echo (Replace %COMPUTERNAME% with your actual computer name or IP)
pause