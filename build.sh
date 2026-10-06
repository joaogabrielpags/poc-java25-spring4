#!/usr/bin/env bash
set -e
export JAVA_HOME=/home/jestevesexit/.sdkman/candidates/java/25.0.2-open
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/jestevesexit/env-dev/git/poc-java25-spring4
java -version
./gradlew "$@"
