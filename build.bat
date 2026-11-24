@echo off
where gradle >nul 2>nul || (echo ยังไม่มี Gradle: ติดตั้ง JDK 17 + Gradle 8.5 ก่อน & pause & exit /b)
gradle build && (echo. & echo สำเร็จ! ไฟล์อยู่ที่ build\libs\xiansky-1.0.0.jar & explorer build\libs)
pause
