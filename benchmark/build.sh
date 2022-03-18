#!/bin/bash
/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

set -e
SCRIPT_DIR=$(dirname "$(realpath $0)")
BENCHMARK=$1
ROOT=$SCRIPT_DIR/..
cd $SCRIPT_DIR
rm -rf runner/out runner/processor-1.0-SNAPSHOT.jar
./gradlew :jar
cd $SCRIPT_DIR/exhaustive-processor
./gradlew build
cp processor/build/libs/processor-1.0-SNAPSHOT.jar ../runner/
cd $SCRIPT_DIR/$BENCHMARK
./build.sh
cd $ROOT
./gradlew -PkspVersion=2.0.255 clean publishAllPublicationsToTestRepository
cp -a build/repos/test/. $ROOT/benchmark/runner
