@echo off
setlocal
set "GRADLE_VERSION=8.10.2"
set "GRADLE_HOME=%~dp0.gradle-local\gradle-%GRADLE_VERSION%"
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
set "ZIP=%TEMP%\gradle-%GRADLE_VERSION%.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
echo Gradle %GRADLE_VERSION% is not installed for this project.
echo Downloading Gradle. This can take a few minutes...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%'"
if errorlevel 1 (
  echo Failed to download Gradle.
  exit /b 1
)
if exist "%~dp0.gradle-local" rmdir /s /q "%~dp0.gradle-local"
mkdir "%~dp0.gradle-local"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%~dp0.gradle-local' -Force"
if errorlevel 1 (
  echo Failed to unpack Gradle.
  exit /b 1
)
del "%ZIP%"
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
