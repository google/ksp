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
cd $SCRIPT_DIR
TEMP_DIR=$(mktemp -d)
BUNDLE=$TEMP_DIR/tachiyomi-exhaustive.tar.gz
tar cfz $BUNDLE tachiyomi lib
BUNDLE_HASH_AND_NAME=$(sha256sum $BUNDLE)
BUNDLE_HASH=$(echo $BUNDLE_HASH_AND_NAME | cut -d " " -f 1)
BUNDLE_UPLOAD_LOCATION=gs://r8-deps/ksp-bench/$BUNDLE_HASH.tar.gz
BUNDLE_DOWNLOAD_LOCATION=http://storage.googleapis.com/r8-deps/ksp-bench/$BUNDLE_HASH.tar.gz
echo Uploading to: $BUNDLE_UPLOAD_LOCATION
gsutil.py cp -a public-read $BUNDLE $BUNDLE_UPLOAD_LOCATION
echo Available for download at: $BUNDLE_DOWNLOAD_LOCATION
