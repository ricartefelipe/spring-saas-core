@REM Maven Wrapper for Windows
@echo off
setlocal
set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"

if exist "%WRAPPER_JAR%" (
  "%JAVA_HOME%\bin\java" -jar "%WRAPPER_JAR%" %*
  goto :eof
)

echo Downloading Maven Wrapper...
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
mkdir "%MAVEN_PROJECTBASEDIR%.mvn\wrapper" 2>nul
powershell -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%' -UseBasicParsing"
"%JAVA_HOME%\bin\java" -jar "%WRAPPER_JAR%" %*
