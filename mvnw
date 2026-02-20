#!/bin/sh
# Maven Wrapper script - uses Maven from PATH or downloads wrapper jar
set -e
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
export MAVEN_PROJECTBASEDIR
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ -r "$WRAPPER_JAR" ]; then
  exec "$(which java)" -jar "$WRAPPER_JAR" "$@"
fi

# Download wrapper if missing
MVNW_VERBOSE=${MVNW_VERBOSE:-false}
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
mkdir -p "$(dirname "$WRAPPER_JAR")"
if $MVNW_VERBOSE; then echo "Downloading Maven Wrapper from $DOWNLOAD_URL"; fi
if command -v wget >/dev/null 2>&1; then
  wget -q -O "$WRAPPER_JAR" "$DOWNLOAD_URL"
elif command -v curl >/dev/null 2>&1; then
  curl -sL -o "$WRAPPER_JAR" "$DOWNLOAD_URL"
else
  echo "Error: need wget or curl to download Maven Wrapper"
  exit 1
fi
exec "$(which java)" -jar "$WRAPPER_JAR" "$@"
