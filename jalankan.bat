@echo off
setlocal
cd /d "%~dp0"
set "JAVA_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN for %%G in (java.exe) do if not "%%~$PATH:G"=="" set "JAVA_BIN=%%~$PATH:G"
if not defined JAVA_BIN goto :no_java
"%JAVA_BIN%" -jar NomorSorter.jar
if errorlevel 1 goto :error
exit /b 0
:no_java
echo Java tidak ditemukan. Pasang JRE / Java Runtime 17 atau lebih baru.
pause
exit /b 1
:error
echo Aplikasi berhenti dengan kesalahan. Salin pesan di atas bila membutuhkan bantuan.
pause
exit /b 1
