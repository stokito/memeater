#!/usr/bin/env bash
free
echo "JAVA_OPTS: $JAVA_OPTS"
echo "args: $@"
java $JAVA_OPTS -XX:+PrintFlagsFinal -verbose:gc -Djava.security.egd=file:/dev/./urandom -jar /app.jar "$@"
