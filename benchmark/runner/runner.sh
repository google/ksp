#!/bin/bash

# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

SCRIPT_DIR=$(dirname "$(realpath $0)")
BENCHMARK_DIR=$1
PREFIX=$2

if [ -z $PREFIX ]
then
  PREFIX=..
fi

JAVA_ARG=$3

if [ -z $JAVA_ARG ]
then
  JAVA=java
else
  JAVA=$(realpath $SCRIPT_DIR/$JAVA_ARG)
fi

BENCHMARK_DATA_DIR=$(realpath $SCRIPT_DIR/$PREFIX/$BENCHMARK_DIR)

echo Running benchmark: $BENCHMARK_DIR
echo With benchmark data directory: $BENCHMARK_DATA_DIR
echo Using java: $JAVA

cd $SCRIPT_DIR
CP=../build/libs/benchmark.jar:$(echo $BENCHMARK_DATA_DIR/lib/*.jar | tr ' ' ':')
KSP_PLUGIN_ID=com.google.devtools.ksp.symbol-processing
KSP_PLUGIN_OPT=plugin:$KSP_PLUGIN_ID

KSP_PLUGIN_JAR=./com/google/devtools/ksp/symbol-processing-cmdline/2.0.255/symbol-processing-cmdline-2.0.255.jar
KSP_API_JAR=./com/google/devtools/ksp/symbol-processing-api/2.0.255/symbol-processing-api-2.0.255.jar

AP=processor-1.0-SNAPSHOT.jar

mkdir -p out
find $BENCHMARK_DATA_DIR -name "*.kt" | xargs $JAVA -cp $CP com.google.devtools.ksp.BenchRunnerKt $BENCHMARK_DIR\
        -kotlin-home . \
        -Xplugin=$KSP_PLUGIN_JAR \
        -Xplugin=$KSP_API_JAR \
        -Xallow-no-source-files \
        -P $KSP_PLUGIN_OPT:apclasspath=$AP \
        -P $KSP_PLUGIN_OPT:projectBaseDir=. \
        -P $KSP_PLUGIN_OPT:classOutputDir=./out \
        -P $KSP_PLUGIN_OPT:javaOutputDir=./out \
        -P $KSP_PLUGIN_OPT:kotlinOutputDir=./out \
        -P $KSP_PLUGIN_OPT:resourceOutputDir=./out \
        -P $KSP_PLUGIN_OPT:kspOutputDir=./out \
        -P $KSP_PLUGIN_OPT:cachesDir=./out \
        -P $KSP_PLUGIN_OPT:incremental=false \
        -P $KSP_PLUGIN_OPT:apoption=key1=value1 \
        -P $KSP_PLUGIN_OPT:apoption=key2=value2 \
        -cp $CP
