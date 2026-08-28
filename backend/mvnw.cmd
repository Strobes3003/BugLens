@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "MAVEN_BASE=%~dp0"
set "MAVEN_CACHE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%MAVEN_CACHE%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%MAVEN_CACHE%\apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven %MAVEN_VERSION% is not cached. Downloading it...
    if not exist "%MAVEN_CACHE%" mkdir "%MAVEN_CACHE%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'; Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_CACHE%' -Force; Remove-Item -LiteralPath '%MAVEN_ZIP%' -Force"
    if errorlevel 1 exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" -f "%MAVEN_BASE%pom.xml" %*
exit /b %errorlevel%
