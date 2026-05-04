@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JAVA_HOME tidak ditemukan di "%JAVA_HOME%".
  exit /b 1
)

if not exist "%~dp0local.properties" (
  echo [WARN] local.properties belum ada. Pastikan android sdk path dikonfigurasi.
  echo [INFO] Contoh isi: sdk.dir=C:\\Users\\Administrator\\AppData\\Local\\Android\\Sdk
)

echo [INFO] Using JAVA_HOME=%JAVA_HOME%
java -version

if "%~1"=="" (
  call gradlew.bat :app:compileMainlineDebugJavaWithJavac :app:compileBanglejsDebugJavaWithJavac
) else (
  call gradlew.bat %*
)

exit /b %ERRORLEVEL%
