@echo off
setlocal
cd /d "%~dp0"
set "JAVA_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN for %%G in (java.exe) do if not "%%~$PATH:G"=="" set "JAVA_BIN=%%~$PATH:G"
if not defined JAVA_BIN (echo Pasang JRE 17 atau lebih baru.& pause & exit /b 1)
"%JAVA_BIN%" -jar NomorSorterUpdater.jar
if errorlevel 1 pause
