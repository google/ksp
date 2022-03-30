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
#

set -e
SCRIPT_DIR=$(dirname "$(realpath $0)")
ROOT=$SCRIPT_DIR/../..
cd $SCRIPT_DIR
git clone https://github.com/inorichi/tachiyomi.git
cd tachiyomi
git checkout 938339690eecdfe309d83264b6a89aff3c767687
git apply $SCRIPT_DIR/tachi.patch
./gradlew :app:copyDepsDevDebug
mkdir -p $SCRIPT_DIR/lib && cp app/build/output/devDebug/lib/* $SCRIPT_DIR/lib
