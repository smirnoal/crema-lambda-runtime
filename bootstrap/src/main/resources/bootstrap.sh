#!/bin/bash

#/var/lang/bin/java \
#   -XX:MaxHeapSize=445645k \
#   -javaagent:/var/runtime/lib/Log4jHotPatch.jar=log4jFixerVerbose=false
#   -XX:+UseSerialGC
#   -Xshare:on -XX:SharedArchiveFile=/var/lang/lib/server/runtime.jsa
#   -XX:+TieredCompilation -XX:TieredStopAtLevel=1
#   --add-opens=java.base/java.io=ALL-UNNAMED
#   -Dorg.crac.Core.Compat=com.amazonaws.services.lambda.crac
#   -XX:+ErrorFileToStderr
#   -Dcom.amazonaws.services.lambda.runtime.api.client.runtimeapi.NativeClient.JNI=/var/runtime/lib/jni/libaws-lambda-jni.linux-x86_64.so
#   -classpath /var/runtime/lib/aws-lambda-java-core-1.2.3.jar:
#         /var/runtime/lib/aws-lambda-java-runtime-interface-client-2.6.0-linux-x86_64.jar:
#         /var/runtime/lib/aws-lambda-java-serialization-1.1.5.jar
#   com.amazonaws.services.lambda.runtime.api.client.AWSLambda helloworld.App::handleRequest





# Add only application jars from /var/task/lib to the classpath.
# Using *.jar avoids picking non-jar files (e.g. AppCDS archives) into classpath.
CLASSPATH=$(printf '%s\n' /var/task/lib/*.jar | sort | paste -sd ':')
#CLASSPATH=$(ls -1 /var/task/lib/*.jar | tr '\n' ':')
CLASSPATH=/var/task:"$CLASSPATH"
export CLASSPATH
exec /var/lang/bin/java \
  -XX:+TieredCompilation -XX:TieredStopAtLevel=1 \
  -XX:+UseSerialGC \
  -XX:+ErrorFileToStderr \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  "$_HANDLER"

#echo "$@"